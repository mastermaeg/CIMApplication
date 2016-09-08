package ch.ninecode.cim.cimweb;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.naming.Context;

import java.util.List;
import java.util.Properties;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;

import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.Interaction;
import javax.resource.cci.MappedRecord;
import javax.resource.cci.Record;

import ch.ninecode.cim.connector.CIMConnectionFactory;
import ch.ninecode.cim.connector.CIMConnectionSpec;
import ch.ninecode.cim.connector.CIMInteractionSpec;
import ch.ninecode.cim.connector.CIMInteractionSpecImpl;
import ch.ninecode.cim.connector.CIMMappedRecord;
import ch.ninecode.cim.connector.CIMResultSet;

@Stateless
@Path("/geovis")
public class GeoVis {
	@Resource(lookup = "openejb:Resource/CIMConnector.rar")
	CIMConnectionFactory factory;

	/**
	 * Build a connection specification used by all the tests.
	 * 
	 * @return
	 */
	CIMConnectionSpec remoteConfig() {
		CIMConnectionSpec ret;

		ret = new CIMConnectionSpec();
		ret.getProperties().put("spark.driver.memory", "1g");
		ret.getProperties().put("spark.executor.memory", "2g");
		ret.getJars().add("C:\\Users\\Markus\\OneDrive\\workspace_win\\CIMScala\\target\\CIMScala-1.6.0-SNAPSHOT.jar");
		ret.getJars().add("C:\\Users\\Markus\\OneDrive\\workspace_win\\CIMApplication\\ShortCircuit\\target\\ShortCircuit-1.0-SNAPSHOT.jar");
		ret.getJars().add("C:\\Users\\Markus\\OneDrive\\workspace_win\\CASGeoVis\\target\\CASGeoVis-0.0.1-SNAPSHOT.jar");
		return (ret);
	}

	private CIMConnectionFactory getFactory() {
		final Properties properties = new Properties();
		CIMConnectionFactory factory = null;
		try {
			Context context = new InitialContext(properties);
			factory = (CIMConnectionFactory) context.lookup("openejb:Resource/CIMConnector.rar");
		} catch (NameNotFoundException nnfe) {
			System.out.println("GeoVis.java.getFactory(), NameNotFoundException");
		} catch (NamingException e) {
			System.out.println("GeoVis.java.getFactory(), NamingException");
		}
		return factory;
	}

	@SuppressWarnings ("unchecked")
    @GET
    @Produces({"text/plain", "application/json"})
    public String GetGeoData (@QueryParam("xmin") String xmin,
    					      @QueryParam("ymin") String ymin,
    					      @QueryParam("xmax") String xmax,
    					      @QueryParam("ymax") String ymax,
    					      @QueryParam("maxLines") String maxLines,
    					      @QueryParam("douglasPeuker") String dougPeuk,
    					      @QueryParam("douglasPeukerFactor") String dougPeukFactor,
    					      @QueryParam("resolution") String resolution,
    					      @QueryParam("reduceLines") String reduceLines)
    {
    	Logger log = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    	log.setLevel(Level.INFO);
    	
    	String filename = "file:///C:/Users/Markus/OneDrive/NIS_CIM.rdf";
        StringBuffer out = new StringBuffer ();
        
        if (factory == null) {
        		factory = getFactory();
        }
        
        if (null != factory)
        {
            Connection connection;
            try
            {
                connection = factory.getConnection (remoteConfig ());
                if (null != connection)
                {
                    try
                    {
                        final CIMInteractionSpecImpl spec = new CIMInteractionSpecImpl ();
                        spec.setFunctionName (CIMInteractionSpec.EXECUTE_METHOD_FUNCTION);
                        final MappedRecord input = factory.getRecordFactory ().createMappedRecord (CIMMappedRecord.INPUT);
                        input.setRecordShortDescription ("record containing the file name and class and method to run");
                        input.put ("filename", filename);
                        input.put ("class", "ch.cas.GeoVis");
                        input.put ("method", "extract");
                        input.put ("xmin", xmin);
                        input.put ("xmax", xmax);
                        input.put ("ymin", ymin);
                        input.put ("ymax", ymax);
                        input.put ("reduceLines", reduceLines);
                        input.put ("maxLines", maxLines);
                        input.put ("dougPeuk", dougPeuk);
                        input.put ("dougPeukFactor", dougPeukFactor);
                        input.put ("resolution", resolution);
                        final Interaction interaction = connection.createInteraction ();
                        final Record output = interaction.execute (spec, input);

                        if ((null == output) || !output.getClass ().isAssignableFrom (CIMResultSet.class))
                            throw new ResourceException ("object of class " + output.getClass ().toGenericString () + " is not a ResultSet");
                        else
                        {
                            CIMResultSet resultset = (CIMResultSet) output;
                            try
                            {
                            	
                                out.append ("{ \"type\": \"FeatureCollection\",\n\"features\": [\n");
                                while (resultset.next ())
                                {
                                	String properties = resultset.getString (1);
                                	String[] propList = properties.substring(1, properties.length() - 1).split(",");
                                	String nisNumber = propList[0];
                                	String name = propList[1];
                                	String aliasName = propList[2];
                                	String location = propList[3];
                                	String baseVoltage = propList[4];
                                	
                                	List<String> coordinates = (List<String>) resultset.getList (2);
                                	String coordString = "";
                                	for (Integer i = 0; i < coordinates.size(); i += 2) {
                                		coordString += "[" + coordinates.get(i) + ", " + coordinates.get(i+1) + "],";
                                	}
                                	coordString = coordString.substring(0, coordString.length() - 1); // get rid of trailing comma
                                	
                                    out.append ("\n{ \"type\": \"Feature\",\n" +
                                        "\"geometry\": {\"type\": \"LineString\", \"coordinates\": [" + coordString + "]},\n" +
                                        "\"properties\": {" +
                                        "\"nis-nummer\": \"" + nisNumber + "\", " +
                                        "\"aliasName\": \"" + aliasName + "\", " +
                                        "\"name\": \"" + name + "\", " +
                                        "\"baseVoltage\": \"" + baseVoltage + "\"" +
                                            "}\n" +
                                        "},");
                                }
                                out.deleteCharAt (out.length () - 1); // get rid of trailing comma
                                out.append ("\n] }\n");
                                resultset.close ();
                            }
                            catch (SQLException sqlexception)
                            {
                                out.append ("SQLException on ResultSet");
                                out.append ("\n");
                                StringWriter string = new StringWriter ();
                                PrintWriter writer = new PrintWriter (string);
                                sqlexception.printStackTrace (writer);
                                out.append (string.toString ());
                                writer.close ();
                            }
                        }
                    }
                    catch (ResourceException resourceexception)
                    {
                        out.append ("ResourceException on interaction");
                        out.append ("\n");
                        StringWriter string = new StringWriter ();
                        PrintWriter writer = new PrintWriter (string);
                        resourceexception.printStackTrace (writer);
                        out.append (string.toString ());
                        writer.close ();
                    }
                    finally
                    {
                        try
                        {
                            connection.close ();
                        }
                        catch (ResourceException resourceexception)
                        {
                            out.append ("ResourceException on close");
                            out.append ("\n");
                            StringWriter string = new StringWriter ();
                            PrintWriter writer = new PrintWriter (string);
                            resourceexception.printStackTrace (writer);
                            out.append (string.toString ());
                            writer.close ();
                        }
                    }
                } else {
                	log.info("connection = null");
                }
            }
            catch (ResourceException exception)
            {
                out.append ("ResourceException");
                out.append ("\n");
                StringWriter string = new StringWriter ();
                PrintWriter writer = new PrintWriter (string);
                exception.printStackTrace (writer);
                out.append (string.toString ());
                writer.close ();
            }
        } else {
        	log.info("factory = null");
        }
        return (out.toString ());
    }
}
