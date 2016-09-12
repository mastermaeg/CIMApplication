package ch.ninecode.sc

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.graphx._
import org.apache.spark.rdd._
import org.apache.spark.sql.SQLContext
import org.apache.spark.storage.StorageLevel
import ch.ninecode._
import ch.ninecode.cim._
import ch.ninecode.model._
import ch.ninecode.sc.ShortCircuit;

object ShortCircuitApplication
{
    def main (args: Array[String])
    {
        // create the configuration
        val configuration = new SparkConf (false)
        configuration.setAppName ("ShortCircuit")
        configuration.setMaster ("spark://sandbox:7077")
        configuration.setSparkHome ("/home/derrick/spark-1.6.0-bin-hadoop2.6/")
        configuration.set ("spark.driver.memory", "1g")
        configuration.set ("spark.executor.memory", "4g")
        configuration.setJars (Array ("/home/derrick/code/CIMScala/target/CIMScala-2.10-1.6.0-1.6.0.jar"
            , "/home/derrick/code/CIMApplication/ShortCircuit/target/ShortCircuit-1.0-SNAPSHOT.jar"
            ))
        // register low level classes
        configuration.registerKryoClasses (Array (classOf[Element], classOf[BasicElement], classOf[Unknown]))
        // register CIM case classes
        CHIM.apply_to_all_classes { x => configuration.registerKryoClasses (Array (x.runtime_class)) }
        // register edge related classes
        configuration.registerKryoClasses (Array (classOf[PreEdge], classOf[Extremum], classOf[ch.ninecode.cim.Edge]))
        // register short circuit classes
        configuration.registerKryoClasses (Array (classOf[ShortCircuitData], classOf[TransformerData], classOf[Message], classOf[VertexData]))
        // register short circuit inner classes
        configuration.registerKryoClasses (Array (classOf[ShortCircuit#EdgePlus], classOf[ShortCircuit#TransformerName], classOf[ShortCircuit#HouseConnection], classOf[ShortCircuit#Result]))

        // make a Spark context and SQL context
        val _Context = new SparkContext (configuration)
        _Context.setLogLevel ("INFO") // Valid log levels include: ALL, DEBUG, ERROR, FATAL, INFO, OFF, TRACE, WARN
        val _SqlContext = new SQLContext (_Context)

        val filename = "hdfs://sandbox:9000/data/" + "NIS_CIM_Export_sias_current_20160816_V8_Bruegg" + ".rdf"

        val start = System.nanoTime ()

        val elements = _SqlContext.read.format ("ch.ninecode.cim").option ("StorageLevel", "MEMORY_AND_DISK_SER").load (filename)
        val count = elements.count

        val read = System.nanoTime ()

        val shortcircuit = new ShortCircuit ()
        shortcircuit._StorageLevel = StorageLevel.MEMORY_AND_DISK_SER
        shortcircuit.preparation (_Context, _SqlContext, "csv=hdfs://sandbox:9000/data/KS_Leistungen.csv")

        val prep = System.nanoTime ()

        val rdd = shortcircuit.stuff (_Context, _SqlContext, "transformer=all") // TRA5401

        val graph = System.nanoTime ()

        val results = rdd.collect

        val fetch = System.nanoTime ()

        println (s"""
        id,Name,ik,ik3pol,ip,Transformer,r,x,r0,x0,wires_valid,trafo_valid,fuse_valid,x,y,fuses""")
        for (i <- 0 until results.length)
        {
            val h = results (i)
            println (h.getString(0) + "," + h.getString(1) + "," + h.getDouble(8) + "," + h.getDouble(9) + "," + h.getDouble(10) + "," + h.getString(2) + "," + h.getDouble(3) + "," + h.getDouble(4) + "," + h.getDouble(5) + "," + h.getDouble(6) + "," + h.getBoolean(11) + "," + h.getBoolean(12) + "," + h.getBoolean(13) + "," + h.getString(14) + "," + h.getString(15) + "," + h.getString(7))
        }

        println ("" + count + " elements")
        println ("read : " + (read - start) / 1e9 + " seconds")
        println ("prep : " + (prep - read) / 1e9 + " seconds")
        println ("graph: " + (graph - prep) / 1e9 + " seconds")
        println ("fetch: " + (fetch - graph) / 1e9 + " seconds")
        println ("print: " + (System.nanoTime () - fetch) / 1e9 + " seconds")
        println ();
    }
}
