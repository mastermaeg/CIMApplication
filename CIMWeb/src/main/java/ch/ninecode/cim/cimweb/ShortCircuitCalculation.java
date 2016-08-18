package ch.ninecode.cim.cimweb;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import javax.naming.Context;
import java.util.Properties;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import javax.resource.ConnectionFactoryDefinition;
import javax.resource.spi.TransactionSupport.TransactionSupportLevel;

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

@ConnectionFactoryDefinition
(
    name = "java:comp/env/eis/SparkConnectionFactory",
    description = "Connection factory for Spark",
    interfaceName = "ch.ninecode.cim.connector.CIMConnectionFactory",
    resourceAdapter = "#CIMConnector", // reference CIMConnector.rar in application.xml
    minPoolSize = 2,
    transactionSupport = TransactionSupportLevel.NoTransaction
)

@Stateless
@Path("/ShortCircuitCalculation/{file}")
public class ShortCircuitCalculation
{
    @Resource (lookup="openejb:Resource/CIMConnector.rar")
    CIMConnectionFactory factory;

    /**
     * Build a connection specification used by all the tests.
     * @return
     */
    CIMConnectionSpec remoteConfig ()
    {
        CIMConnectionSpec ret;

        ret = new CIMConnectionSpec ();
//        ret.setUserName ("derrick"); // not currently used
//        ret.setPassword ("secret"); // not currently used
        ret.getProperties ().put ("spark.driver.memory", "1g");
        ret.getProperties ().put ("spark.executor.memory", "4g");
        //ret.getJars ().add ("/home/derrick/code/CIMScala/target/CIMScala-1.6.0-SNAPSHOT.jar");
        //ret.getJars ().add ("/home/derrick/code/CIMApplication/ShortCircuit/target/ShortCircuit-1.0-SNAPSHOT.jar");
        ret.getJars ().add ("/home/maeg/nis/CIMScala-1.6.0-SNAPSHOT.jar");
        ret.getJars ().add ("/home/maeg/nis/ShortCircuit-1.0-SNAPSHOT.jar");
        return (ret);
    }

    @SuppressWarnings ("unchecked")
    @GET
    @Path("{p:/?}{item:((.*)?)}")
    @Produces ({"text/plain", "application/json"})
    public String GetShortCircuitData (@PathParam("file") String filename, @PathParam("item") String item)
    {
    	Logger log = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    	log.setLevel(Level.INFO);
    	log.info("GetShortCircuitData, logger log.servere");
    	
        String transformer = (null != item && !item.equals ("")) ? item : null;
        StringBuffer out = new StringBuffer ();
        
        if (factory == null) {
        	{
                final Properties properties = new Properties ();
                try
                {
                    Context context = new InitialContext (properties);
                    factory = (CIMConnectionFactory) context.lookup ("openejb:Resource/CIMConnector.rar");
                }
                catch (NameNotFoundException nnfe)
                {
                    out.append ("NameNotFoundException");
                    out.append ("\n");
                    StringWriter string = new StringWriter ();
                    PrintWriter writer = new PrintWriter (string);
                    nnfe.printStackTrace (writer);
                    out.append (string.toString ());
                    writer.close ();
                }
                catch (NamingException e)
                {
                    out.append ("NamingException");
                    out.append ("\n");
                    StringWriter string = new StringWriter ();
                    PrintWriter writer = new PrintWriter (string);
                    e.printStackTrace (writer);
                    out.append (string.toString ());
                    writer.close ();
                }
            };
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
                        String full_file = "hdfs://sandbox:8020/user/maeg/NIS/NIS_CIM.rdf";
                        final CIMInteractionSpecImpl spec = new CIMInteractionSpecImpl ();
                        spec.setFunctionName (CIMInteractionSpec.EXECUTE_METHOD_FUNCTION);
                        final MappedRecord input = factory.getRecordFactory ().createMappedRecord (CIMMappedRecord.INPUT);
                        input.setRecordShortDescription ("record containing the file name and class and method to run");
                        input.put ("filename", full_file);
                        input.put ("class", "ch.ninecode.cim.ShortCircuit");
                        if (null == transformer)
                            input.put ("method", "preparation");
                        else
                            input.put ("method", "stuff");
                        if (null != transformer)
                            input.put ("transformer", transformer);
                        final Interaction interaction = connection.createInteraction ();
                        final Record output = interaction.execute (spec, input);
                        if ((null == output) || !output.getClass ().isAssignableFrom (CIMResultSet.class))
                            throw new ResourceException ("object of class " + output.getClass ().toGenericString () + " is not a ResultSet");
                        else
                        {
                            CIMResultSet resultset = (CIMResultSet)output;
                            try
                            {
                                if (null != transformer)
                                {
                                    out.append ("{ \"type\": \"FeatureCollection\",\n\"features\": [\n");
                                    while (resultset.next ())
                                    {
                                        out.append ("\n{ \"type\": \"Feature\",\n" +
                                            "\"geometry\": {\"type\": \"Point\", \"coordinates\": [" + resultset.getString (15) + ", " + resultset.getString (16) + "]},\n" +
                                            "\"properties\": {" +
                                            "\"mRID\": \"" + resultset.getString (1) + "\", " +
                                            "\"node\": \"" + resultset.getString (2) + "\", " +
                                            "\"transformer\": \"" + resultset.getString (3) + "\", " +
                                            "\"r\": \"" + resultset.getDouble (4) + "\", " +
                                            "\"x\": \"" + resultset.getDouble (5) + "\", " +
                                            "\"r0\": \"" + resultset.getDouble (6) + "\", " +
                                            "\"x0\": \"" + resultset.getDouble (7) + "\", " +
                                            "\"fuses\": \"" + resultset.getString (8) + "\", " +
                                            "\"ik\": \"" + resultset.getDouble (9) + "\", " +
                                            "\"ik3pol\": \"" + resultset.getDouble (10) + "\", " +
                                            "\"ip\": \"" + resultset.getDouble (11) + "\", " +
                                            "\"wires_valid\": " + resultset.getBoolean (12) + ", " +
                                            "\"trafo_valid\": " + resultset.getBoolean (13) + ", " +
                                            "\"fuse_valid\": " + resultset.getBoolean (14) +
                                                "}\n" +
                                            "},");
                                    }
                                    out.deleteCharAt (out.length () - 1); // get rid of trailing comma
                                    out.append ("\n] }\n");
                                }
                                else
                                {
                                    out.append ("[\n");
                                    while (resultset.next ())
                                        out.append ("\"" + resultset.getString (1) + "\",\n");
                                    out.deleteCharAt (out.length () - 1); // get rid of trailing newline
                                    out.deleteCharAt (out.length () - 1); // get rid of trailing comma
                                    out.append ("\n]\n");
                                }
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
                        interaction.close ();
                        connection.close ();
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
