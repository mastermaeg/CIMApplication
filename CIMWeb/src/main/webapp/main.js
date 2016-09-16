/**
 * Created by marku_000 on 06.07.2016.
 */


var vectorSource;
var lastLoadedResolution;
var map;

var onInit = function () {
	
    var vectorLayer = this.createVectorLayer();
    var customControls = this.createCustomControls();
    this.createMap(vectorLayer, customControls);
	
	map.on('click', function(evt) {
		this.displayFeatureInfo(evt.pixel);
	}, this);

};

var createMap = function (vectorLayer, customControls) {
    map = new ol.Map({
        target: 'map',
        controls: ol.control.defaults({
            attributionOptions: /** @type {olx.control.AttributionOptions} */ ({
                collapsible: false
            })
        }).extend(customControls),
        layers: [
            new ol.layer.Tile({
                source: new ol.source.OSM(),
				opacity: 0.7
            }),
            vectorLayer
        ],
        view: new ol.View({
            projection: 'EPSG:4326',
            center: ol.proj.fromLonLat([8.66299086783, 46.9771651140], 'EPSG:4326'),
            zoom: 12
        })
    });
};

var createVectorLayer = function () {

    var image = new ol.style.Circle({
        radius: 5,
        fill: null,
        stroke: new ol.style.Stroke({color: 'red', width: 1})
    });

    var styles = {
        'Point': new ol.style.Style({
            image: image
        }),
        'LineString': new ol.style.Style({
            stroke: new ol.style.Stroke({
                color: 'red',
                width: 5
            })
        }),
    };

    var styleFunction = function(feature) {
        return styles[feature.getGeometry().getType()];
    };

	var geoJsonFormat =  new ol.format.GeoJSON();
	vectorSource = new ol.source.Vector({
	  format: geoJsonFormat,
	  loader: function(extent, resolution, projection) {
		var params = {
			xmin: extent[0],
			xmax: extent[2],
			ymin: extent[1],
			ymax: extent[3],
			reduceLines: document.getElementById("numFlag").checked,
			maxLines: document.getElementById("numInput").value,
			douglasPeuker: document.getElementById("dpFlag").checked,
			douglasPeukerFactor: document.getElementById("dpFactor").value, 
			resolution: resolution
		};
		var url = 'cim/geovis'
		$.ajax({
		  url: url,
		  data: params,
		  success: function(data) {
			vectorSource.addFeatures(geoJsonFormat.readFeatures(data));
		  }
		}); 
	  },
	  projection: 'EPSG:4326',
	  //strategy: ol.loadingstrategy.bbox
	  strategy: function(extent, resolution) {
		  if (lastLoadedResolution > resolution) {
			  vectorSource.clear(false);
		  }
		  lastLoadedResolution = resolution;
		  return [extent];
	  },
	});

    var vectorLayer = new ol.layer.Vector({
        source: vectorSource,
        style: styleFunction
    });

    return vectorLayer;
};

var createCustomControls = function() {
    var mousePositionControl = new ol.control.MousePosition({
        coordinateFormat: ol.coordinate.createStringXY(4),
        projection: 'EPSG:4326',
        // comment the following two lines to have the mouse position
        // be placed within the map.
        className: 'custom-mouse-position',
        target: document.getElementById('mouse-position'),
        target: document.getElementById('mouse-position'),
        undefinedHTML: 'undefinded'
    });

    return [mousePositionControl]
}

var displayFeatureInfo = function(pixel) {

	var feature = map.forEachFeatureAtPixel(pixel, function(feature, layer) {
		return feature;
	});

	var info = document.getElementById('info');
	if (feature) {
		var html = '<table>';
		html += '<tr><td>Name</td><td>' + feature.get("name") + '</td></tr>';
		html += '<tr><td>AliasName</td><td>' + feature.get("aliasName") + '</td></tr>';
		html += '<tr><td>Spannungsebene</td><td>' + feature.get("baseVoltage") + '</td></tr>';
		html += '<tr><td>NIS-Nummer</td><td>' + feature.get("nis-nummer") + '</td></tr>';
		html += '</table>';
		info.innerHTML = html;
	} else {
		info.innerHTML = '&nbsp;';
	}
};

var changeMaxLines = function() {
	vectorSource.clear();
}