package ch.ninecode.cim.cimweb;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.Date;

@Path("/geovis")
public class GeoVis {

    @GET
    public String getLines() {
        
    	
    	return "pojo ok @ " + new Date().toString();
    }
}
