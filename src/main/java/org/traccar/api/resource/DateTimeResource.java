package org.traccar.api.resource;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.json.Json;
import jakarta.json.JsonObject;

@Path("datetime")
@Produces(MediaType.APPLICATION_JSON)
public class DateTimeResource {

    @GET
    public JsonObject getDateTime() {
        ZonedDateTime currentDateTime = ZonedDateTime.now().withZoneSameInstant(java.time.ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        String formattedDateTime = currentDateTime.format(formatter);

        return Json.createObjectBuilder()
                .add("datetime", formattedDateTime)
                .build();
    }
}
