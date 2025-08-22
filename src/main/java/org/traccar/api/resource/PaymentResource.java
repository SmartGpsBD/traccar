package org.traccar.api.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONObject;
import org.traccar.model.PaymentRequest;
import org.traccar.model.Device;
import org.traccar.model.Driver;
import org.traccar.model.Permission;
import org.traccar.model.User;
import org.traccar.storage.Storage;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Path("paymentverify")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PaymentResource {

    @Context
    private HttpServletRequest request;

    @Inject
    private Storage storage;

    private static final HttpClient httpClient = HttpClient.newHttpClient();

    @POST
    public Response verifyPayment(PaymentRequest requestData) throws Exception {

        // Step 1: Call bKash execute payment API using paymentID
        HttpRequest bkashRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://tokenized.pay.bka.sh/v1.2.0-beta/tokenized/checkout/execute"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", requestData.getGrantToken())
                .header("x-app-key", "MfKACQbsmyLarBdiPJk6RGLHtc")
                .POST(HttpRequest.BodyPublishers.ofString(new JSONObject().put("paymentID", requestData.getPaymentId()).toString()))
                .build();

        HttpResponse<String> bkashResponse = httpClient.send(bkashRequest, HttpResponse.BodyHandlers.ofString());

        // Parse response JSON
        JSONObject responseJson = new JSONObject(bkashResponse.body());
        String statusMessage = responseJson.optString("statusMessage");
        String trxID = responseJson.optString("trxID");
        String amount = responseJson.optString("amount");

        // Check condition for successful payment
        boolean isSuccessful = "Successful".equalsIgnoreCase(statusMessage) && trxID != null && !trxID.isEmpty();

        if (isSuccessful) {
            // Step 2: Update expiration time
            Device device = storage.getObject(Device.class, new Request(
                    new Columns.All(),
                    new Condition.Equals("id", requestData.getId())));
            device.setExpirationTime(requestData.getExpirationTime());
            storage.updateObject(device, new Request(
                    new Columns.Include("expirationTime"),
                    new Condition.Equals("id", device.getId())));

            // Step 3: Save payment history to Driver table
            Driver driver = new Driver();
            driver.setName(requestData.getName());
            driver.setAmount(amount);
            driver.setUniqueId(trxID);
            driver.setValidity(requestData.getValidity());
            driver.setTime(requestData.getTime());

            long driverId = storage.addObject(driver, new Request(new Columns.All()));

            Permission adminPermission = new Permission(User.class, 4, Driver.class, driverId);
            storage.addPermission(adminPermission);

            Permission userPermission = new Permission(User.class, requestData.getUserId(), Driver.class, driverId);
            storage.addPermission(userPermission);

            // Step 4: Return raw bKash response
            return Response.ok(responseJson.toMap()).build();
        }

        // Payment failed: return raw response as error
        return Response.status(Response.Status.BAD_REQUEST).entity(responseJson.toMap()).build();
    }
}
