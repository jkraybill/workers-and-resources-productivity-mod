var nationName;

var tempFileOutput;

// industries
var wood;
var iron;
var coal;
var gravel;
var oil;
var agriculture;
var alcohol;
var energy;
var cm;
var livestock;
var acm;
var clothing;

var SD; // building sdev

var mapFile;

function createArray(length) {
    var arr = new Array(length || 0),
        i = length;

    if (arguments.length > 1) {
        var args = Array.prototype.slice.call(arguments, 1);
        while(i--) arr[length-1 - i] = createArray.apply(this, args);
    }

    return arr;
}

function downloadZip(zip) {
	// create the .tsv file
	var txt = "";
	for (var r = 5; r < 34; r++) {
		for (var c = 1; c < 9; c++) {
			console.log(r + "/" + c);
			var val = mapFile[r][c];
			if (!val) {
				txt += "\t";
			} else {
				txt += val;
				if (c != 8) {
					txt += "\t";
				}
			}
		}
		txt += "\n";
	}

	zip.file("data.tsv", txt);

	zip.generateAsync({type:"blob"}).then(function (blob) {
		saveAs(blob, nationName + ".zip");
	});
}

var totalFiles = 0;
var totalFilesRemaining = 0;

function calcFromMinMax() {
	if (parseInt($("#maxProd").val()) < parseInt($("#minProd").val())) {
		$("#maxProd").val($("#minProd").val()).change();
	}
	var prodRange = parseInt($("#maxProd").val()) - parseInt($("#minProd").val());

	$("#industryMean").val(Math.round((prodRange / 2) + parseInt($("#minProd").val()))).change();

	$("#industrySd").val(Math.round((prodRange / 4) * 1.5)).change();

	$("#bldgSd").val(Math.round((prodRange / 4) * 0.5)).change();
}

var industries = [];

var chartClone;

function generateScenario() {
	industries = [];

	wood = new Industry("Wood", "rgba(98, 57, 46, 0.4)", "rgb(98, 57, 46)");
	industries.push(wood);

	iron = new Industry("Iron", "rgba(201, 203, 207, 0.4)", "rgb(201, 203, 207)");
	industries.push(iron);

	coal = new Industry("Coal", "rgba(22, 22, 22, 0.4)", "rgb(44, 44, 44)");
	industries.push(coal);

	gravel = new Industry("Gravel", "rgba(199, 227, 204, 0.4)", "rgb(199, 227, 204)");
	industries.push(gravel);

	oil = new Industry("Oil", "rgba(22, 35, 102, 0.4)", "rgb(22, 35, 102)");
	industries.push(oil);

	agriculture = new Industry("Agriculture", "rgba(238, 155, 82, 0.4)", "rgb(1238, 155, 82)");
	industries.push(agriculture);

	alcohol = new Industry("Alcohol", "rgba(134, 212, 210, 0.4)", "rgb(134, 212, 210)");
	industries.push(alcohol);

	energy = new Industry("Energy", "rgba(204, 75, 136, 0.4)", "rgb(204, 75, 136)");
	industries.push(energy);

	cm = new Industry("Construction Materials", "rgba(104, 75, 136, 0.4)", "rgb(104, 75, 136)");
	industries.push(cm);

	livestock = new Industry("Livestock", "rgba(227, 93, 52, 0.4)", "rgb(227, 93, 52)");
	industries.push(livestock);

	acm = new Industry("Advanced Construction Materials", "rgba(121, 46, 146, 0.4)", "rgb(121, 46, 146)");
	industries.push(acm);

	clothing = new Industry("Clothing", "rgba(52, 115, 168, 0.4)", "rgb(52, 115, 168)");
	industries.push(clothing);

	industries.sort(function(a, b) {
		return b.fame - a.fame;
	});
	console.log(industries);

	var chartData = [];
	var chartLabels = [];
	var chartColors = [];
	var chartLines = [];

	for (i = 0; i < industries.length; i++) {
		chartData.push(industries[i].productivity * 100);
		chartData.push(industries[i].size * 100);
		chartLabels.push(industries[i].name + ": productivity");
		chartLabels.push(industries[i].name + ": size");
		chartColors.push(industries[i].color);
		chartColors.push(industries[i].color);
		chartLines.push(industries[i].linecolor);
		chartLines.push(industries[i].linecolor);
	}

	// now gen the chart
	$("#chart").replaceWith(chartClone.clone());

	new Chart(document.getElementById("chart"), {
		"type": "horizontalBar",
		"data": {
			"labels": chartLabels,
			"datasets": [{
				"data": chartData,
				"fill": false,
				"backgroundColor": chartColors,
				"borderColor": chartLines,
				"borderWidth": 1
			}]
		},
		"options": {
			"scales": {
				"xAxes": [{
					"ticks": {
						"beginAtZero": true
					}
				}]
			},
			"legend": {
				display: false
			}
		}
	});

	nationName = getNationName();
	$(".nationName").html(nationName);
	$(".scenario").css("opacity", "1.0");
	$(".step3").css("opacity", "1.0");

}

$(function() {
	chartClone = $("#chart").clone();

	$('[data-toggle="tooltip"]').tooltip();

	$('input[type=range]').on('input', function () {
		$(this).trigger('change');
	});

	$("#minProd").change(function() {
		$("#minProdVal").html($("#minProd").val() + "%");
		calcFromMinMax();
	});

	$("#maxProd").change(function() {
		$("#maxProdVal").html($("#maxProd").val() + "%");
		calcFromMinMax();
	});

	$("#industryMean").change(function() {
		$("#industryMeanVal").html($("#industryMean").val() + "%");
	});

	$("#industrySd").change(function() {
		$("#industrySdVal").html($("#industrySd").val() + "%");
	});

	$("#bldgSd").change(function() {
		$("#bldgSdVal").html($("#bldgSd").val() + "%");
	});

	$("#genButton").click(generateScenario);

	$("#file").on("change", function(evt) {

		mapFile = createArray(34, 10);

		SD = parseFloat($("#bldgSd").val()) / 100;
		var files = evt.target.files;
		totalFiles = files.length;
		totalFilesRemaining = files.length;
		console.log("handling " + totalFilesRemaining + " uploaded files.");

		// Closure to capture the file information.
		var zip = new JSZip();

		function handleFile(f) {
			console.log("handling " + f.name);
			var input = event.target;

			var reader = new FileReader();
			reader.onload = function() {
				var text = reader.result;
				var newtext = processFile(f.name, text);

				if (newtext) { // only include files we processed.
					zip.file(f.name + ".bak", text);
					zip.file(f.name, newtext);
				}
				if (--totalFilesRemaining == 0) {
					$(".step4").css("opacity", "1.0");
					$(".step5").css("opacity", "1.0");
					downloadZip(zip);
					$("#file").val("");
				}
				console.log(totalFilesRemaining + " files remaining to process.");
			};
			reader.readAsText(f);
		}

		for (var i = 0; i < files.length; i++) {
			handleFile(files[i]);
		}
	});

});

function modifyWorkers(ind, text, key, mean) {
	return modify(text, key, mean * ind.size, mean * ind.size * SD, 1, df0, "", 1);
}

function modifyProduction(ind, text, key, mean) {
	return modify(text, key, mean, SD * mean, ind.productivity, df3, "", 1);
}

function modifyConsumption(ind, text, key, mean) {
	return modify(text, key, mean, SD * mean, 1, df3, "", 1);
}

function modifyConstruction(ind, text, key, mean) {
	return modifyConstructionImpl(ind, text, key, mean, "", 1);
}

function modifyConstructionImpl(ind, text, key, mean, suffix, matchIndex) {
	return modify(text, key, mean, SD * mean, ind.fame, df3, suffix, matchIndex);
}

function modify(body, key, mean, deviationUnit, multiplier, df, suffix, matchIndex) {

	var gauss = gaussian(mean, deviationUnit);
	var newval = Math.max(mean * 0.1, gauss) * multiplier; // don't accept gaussian values less than 10% of original (esp not negative ones)

	suffix = (suffix === "") ? "" : (" " + suffix);
	var t = 0;
	body = body.replace(new RegExp(RegExp.escape(key) + ".*", "g"), function (match) {
		t++;
		return (t === matchIndex) ? (key + " " + df.format(newval) + suffix) : match;
	});

	console.log(key + ": " + df.format(newval) + suffix + " (" + df2.format(gauss) + " against mean " + df2.format(mean) + " sd " + df2.format(deviationUnit) + " and multiplier " + df2.format(multiplier) + ")");
	tempFileOutput = newval;
	return body;
}

var NAME1 = [ "Udzek", "Turj", "Czev", "Tar", "Ala", "Ak", "Ar", "Uk", "Ug", "Bur", "Ro", "Mol", "Es", "Aker" ];
var NAME2 = [ "", "men", "ik", "ban", "ghan", "slav", "oslav", "oslov", "stotz", "dov", "on", "v" ];
var NAME3 = [ "ia", "istan", "akia", "a" ];

function getNationName() {
	return getRandomArrayElement(NAME1) + getRandomArrayElement(NAME2) + getRandomArrayElement(NAME3);
}

function map(rowid, colid, s) {
	mapFile[rowid][colid] = s;
}

function processFile(filename, text) {

	console.log("Processing " + filename);
	if (filename == "woodcutting_post.ini") {
		text = modifyWorkers(wood, text, "$WORKERS_NEEDED", 30);
		var w = tempFileOutput;
		text = modifyProduction(wood, text, "$PRODUCTION wood", 6.25);
		var p1 = tempFileOutput;
		text = modifyConstruction(wood, text, "$STORAGE_EXPORT RESOURCE_TRANSPORT_OPEN", 50);
		text = modifyConstruction(wood, text, "$COST_RESOURCE_AUTO ground_asphalt", 1);
		text = modifyConstruction(wood, text, "$COST_RESOURCE_AUTO wall_wood", 0.8);
		text = modifyConstruction(wood, text, "$COST_RESOURCE_AUTO roof_woodsteel", 0.8);
		map(5, 1, "Woodcutting Post");
		map(5, 2, w);
		map(5, 7, w * p1);
		return text;
	} else if (filename == "sawmill.ini") {
		text = modifyWorkers(wood, text, "$WORKERS_NEEDED", 20);
		var w = tempFileOutput;
		text = modifyProduction(wood, text, "$PRODUCTION boards", 7);
		var p1 = tempFileOutput;
		text = modifyConsumption(wood, text, "$CONSUMPTION wood", 9);
		var c1 = tempFileOutput;
		text = modifyConstruction(wood, text, "$CONSUMPTION_PER_SECOND eletric", 0.027);
		text = modifyConstruction(wood, text, "$STORAGE_IMPORT RESOURCE_TRANSPORT_OPEN", 60);
		text = modifyConstruction(wood, text, "$STORAGE_EXPORT RESOURCE_TRANSPORT_OPEN", 50);
		text = modifyConstruction(wood, text, "$COST_RESOURCE_AUTO ground_asphalt", 1);
		text = modifyConstruction(wood, text, "$COST_RESOURCE_AUTO wall_brick", 1);
		text = modifyConstruction(wood, text, "$COST_RESOURCE_AUTO roof_woodsteel", 1);
		map(6, 1, "Sawmill");
		map(6, 2, w);
		map(6, 3, w * c1);
		map(6, 7, w * p1);
		return text;
	} else if (filename == "iron_mine.ini") {
		text = modifyProduction(iron, text, "$PRODUCTION rawiron", 4);
		var p1 = tempFileOutput;
		text = modifyConstruction(iron, text, "$STORAGE_EXPORT RESOURCE_TRANSPORT_GRAVEL", 20);
		text = modifyWorkers(iron, text, "$WORKERS_NEEDED", 250);
		var w = tempFileOutput;
		text = modifyConstruction(iron, text, "$COST_RESOURCE_AUTO ground_asphalt", 1);
		text = modifyConstruction(iron, text, "$COST_RESOURCE_AUTO wall_concrete", 1);
		text = modifyConstruction(iron, text, "$COST_RESOURCE_AUTO tech_steel", 0.8);
		text = modifyConstruction(iron, text, "$COST_RESOURCE workers", 3050);
		text = modifyConstruction(iron, text, "$COST_RESOURCE boards", 75);
		text = modifyConstruction(iron, text, "$COST_RESOURCE concrete", 180);
		text = modifyConstruction(iron, text, "$COST_RESOURCE steel", 45);
		map(8, 1, "Iron Mine");
		map(8, 2, w);
		map(8, 7, w * p1);
		return text;
	} else if (filename == "iron_processing.ini") {
		text = modifyWorkers(iron, text, "$WORKERS_NEEDED", 15);
		var w = tempFileOutput;
		text = modifyProduction(iron, text, "$PRODUCTION iron", 7);
		var p1 = tempFileOutput;
		text = modifyConsumption(iron, text, "$CONSUMPTION rawiron", 15);
		var c1 = tempFileOutput;
		text = modifyConstruction(iron, text, "$CONSUMPTION_PER_SECOND eletric", 0.28);
		text = modifyConstruction(iron, text, "$STORAGE_IMPORT RESOURCE_TRANSPORT_GRAVEL", 25);
		text = modifyConstruction(iron, text, "$STORAGE_EXPORT RESOURCE_TRANSPORT_GRAVEL", 25);
		text = modifyConstruction(iron, text, "$COST_RESOURCE_AUTO ground_asphalt", 1);
		text = modifyConstruction(iron, text, "$COST_RESOURCE_AUTO wall_concrete", 1);
		text = modifyConstruction(iron, text, "$COST_RESOURCE_AUTO wall_steel", 1);
		text = modifyConstruction(iron, text, "$COST_RESOURCE_AUTO tech_steel", 1);
		map(9, 1, "Iron Processing");
		map(9, 2, w);
		map(9, 3, w * c1);
		map(9, 7, w * p1);
		return text;
	} else if (filename == "coal_mine.ini") {
		text = modifyProduction(coal, text, "$PRODUCTION rawcoal", 4.2);
		var p1 = tempFileOutput;
		text = modifyConstruction(coal, text, "$STORAGE_EXPORT RESOURCE_TRANSPORT_GRAVEL", 20);
		text = modifyWorkers(coal, text, "$WORKERS_NEEDED", 220);
		var w = tempFileOutput;
		text = modifyConstruction(coal, text, "$COST_RESOURCE_AUTO ground_asphalt", 1);
		text = modifyConstruction(coal, text, "$COST_RESOURCE_AUTO wall_concrete", 1);
		text = modifyConstruction(coal, text, "$COST_RESOURCE_AUTO tech_steel", 0.8);
		text = modifyConstruction(coal, text, "$COST_RESOURCE workers", 3000);
		text = modifyConstruction(coal, text, "$COST_RESOURCE boards", 75);
		text = modifyConstruction(coal, text, "$COST_RESOURCE concrete", 180);
		text = modifyConstruction(coal, text, "$COST_RESOURCE steel", 45);
		map(11, 1, "Coal Mine");
		map(11, 2, w);
		map(11, 7, w * p1);
		return text;
	} else if (filename == "coal_processing.ini") {
		text = modifyWorkers(coal, text, "$WORKERS_NEEDED", 30); // was 15
		var w = tempFileOutput;
		text = modifyProduction(coal, text, "$PRODUCTION coal", 2.5); // was 8
		var p1 = tempFileOutput;
		text = modifyConsumption(coal, text, "$CONSUMPTION rawcoal", 7);
		var c1 = tempFileOutput;
		text = modifyConstruction(coal, text, "$CONSUMPTION_PER_SECOND eletric", 0.25);
		text = modifyConstruction(coal, text, "$STORAGE_IMPORT RESOURCE_TRANSPORT_GRAVEL", 25);
		text = modifyConstruction(coal, text, "$STORAGE_EXPORT RESOURCE_TRANSPORT_GRAVEL", 25);
		text = modifyConstruction(coal, text, "$COST_RESOURCE_AUTO ground_asphalt", 1);
		text = modifyConstruction(coal, text, "$COST_RESOURCE_AUTO wall_concrete", 1);
		text = modifyConstruction(coal, text, "$COST_RESOURCE_AUTO wall_steel", 1);
		text = modifyConstruction(coal, text, "$COST_RESOURCE_AUTO tech_steel", 1);
		map(12, 1, "Coal Processing");
		map(12, 2, w);
		map(12, 3, w * c1);
		map(12, 7, w * p1);
		return text;
	} else if (filename == "gravelmine.ini") {
		text = modifyWorkers(gravel, text, "$WORKERS_NEEDED", 40);
		var w = tempFileOutput;
		text = modifyProduction(gravel, text, "$PRODUCTION rawgravel", 3.5);
		var p1 = tempFileOutput;
		text = modifyConstruction(gravel, text, "$STORAGE_EXPORT RESOURCE_TRANSPORT_GRAVEL", 0.5);
		text = modifyConstruction(gravel, text, "$COST_RESOURCE_AUTO ground_asphalt", 3);
		text = modifyConstruction(gravel, text, "$COST_RESOURCE_AUTO wall_brick", 2.8);
		text = modifyConstruction(gravel, text, "$COST_RESOURCE_AUTO roof_woodsteel", 2.8);
		map(15, 1, "Quarry");
		map(15, 2, w);
		map(15, 7, w * p1);
		return text;
	} else if (filename == "gravel_processing.ini") {
		text = modifyWorkers(gravel, text, "$WORKERS_NEEDED", 15);
		var w = tempFileOutput;
		text = modifyProduction(gravel, text, "$PRODUCTION gravel", 5.5);
		var p1 = tempFileOutput;
		text = modifyConsumption(gravel, text, "$CONSUMPTION rawgravel", 8);
		var c1 = tempFileOutput;
		text = modifyConstruction(gravel, text, "$CONSUMPTION_PER_SECOND eletric", 0.4);
		text = modifyConstruction(gravel, text, "$COST_RESOURCE_AUTO ground_asphalt", 1);
		text = modifyConstruction(gravel, text, "$COST_RESOURCE_AUTO wall_concrete", 0.8);
		text = modifyConstruction(gravel, text, "$COST_RESOURCE_AUTO wall_steel", 0.35);
		text = modifyConstruction(gravel, text, "$COST_RESOURCE_AUTO tech_steel", 0.35);
		text = modifyConstruction(gravel, text, "$STORAGE_IMPORT RESOURCE_TRANSPORT_GRAVEL", 10);
		text = modifyConstruction(gravel, text, "$STORAGE_EXPORT RESOURCE_TRANSPORT_GRAVEL", 145);
		map(16, 1, "Gravel Processing");
		map(16, 2, w);
		map(16, 3, w * c1);
		map(16, 7, w * p1);
		return text;
	} else if (filename == "oil_mine.ini") {
		text = modifyProduction(oil, text, "$PRODUCTION oil", 3.5); // was 7
		var p1 = tempFileOutput;
		text = modifyConstruction(oil, text, "$CONSUMPTION_PER_SECOND eletric", 0.065);
		text = modifyConstruction(oil, text, "$STORAGE_EXPORT RESOURCE_TRANSPORT_OIL", 15);
		text = modifyConstruction(oil, text, "$COST_RESOURCE_AUTO ground_asphalt", 1);
		text = modifyConstruction(oil, text, "$COST_RESOURCE_AUTO tech_steel", 2);
		map(19, 1, "Oil Rig");
		map(19, 2, 1); // "1" worker
		map(19, 7, p1);
		return text;
	} else if (filename == "oil_rafinery.ini") {
		text = modifyWorkers(oil, text, "$WORKERS_NEEDED", 500);
		var w = tempFileOutput;
		text = modifyProduction(oil, text, "$PRODUCTION fuel", 0.2); // was 0.25
		var p1 = tempFileOutput;
		text = modifyProduction(oil, text, "$PRODUCTION bitumen", 0.12); // was 0.15
		var p2 = tempFileOutput;
		text = modifyConsumption(oil, text, "$CONSUMPTION oil", 0.5);
		var c1 = tempFileOutput;
		text = modifyConstruction(oil, text, "$CONSUMPTION_PER_SECOND eletric", 0.21);
		text = modifyConstruction(oil, text, "$STORAGE_IMPORT RESOURCE_TRANSPORT_OIL", 450);
		text = modifyConstructionImpl(oil, text, "$STORAGE_EXPORT_SPECIAL RESOURCE_TRANSPORT_OIL", 340, "fuel", 1);
		text = modifyConstructionImpl(oil, text, "$STORAGE_EXPORT_SPECIAL RESOURCE_TRANSPORT_OIL", 250, "bitumen", 2);
		text = modifyConstruction(oil, text, "$COST_RESOURCE_AUTO ground_asphalt", 1);
		text = modifyConstruction(oil, text, "$COST_RESOURCE_AUTO tech_steel", 0.8);
		map(20, 1, "Oil Refinery");
		map(20, 2, w);
		map(20, 3, w * c1);
		map(20, 7, w * p1);
		map(20, 8, w * p2);
		return text;
	} else if (filename == "farm.ini") {
		text = modifyProduction(agriculture, text, "$PRODUCTION plants", 0.01);
		var p1 = tempFileOutput;
		text = modifyConstruction(agriculture, text, "$STORAGE_EXPORT RESOURCE_TRANSPORT_COVERED", 170);
		text = modifyConstruction(agriculture, text, "$COST_RESOURCE_AUTO ground_asphalt", 0.6);
		text = modifyConstruction(agriculture, text, "$COST_RESOURCE_AUTO wall_steel", 0.6);
		text = modifyConstruction(agriculture, text, "$COST_RESOURCE_AUTO tech_steel", 0.5);
		map(7, 1, "Farm");
		map(7, 2, 1); // "1 worker"
		map(7, 7, 250 * p1);
		return text;
	} else if (filename == "food_factory.ini") {
		text = modifyWorkers(agriculture, text, "$WORKERS_NEEDED", 170);
		var w = tempFileOutput;
		text = modifyProduction(agriculture, text, "$PRODUCTION food", 0.12);
		var p1 = tempFileOutput;
		text = modifyConsumption(agriculture, text, "$CONSUMPTION plants", 0.25);
		var c1 = tempFileOutput;
		text = modifyConstruction(agriculture, text, "$COST_RESOURCE_AUTO ground_asphalt", 1);
		text = modifyConstruction(agriculture, text, "$COST_RESOURCE_AUTO wall_brick", 0.8);
		text = modifyConstruction(agriculture, text, "$COST_RESOURCE_AUTO wall_steel", 0.2);
		text = modifyConstruction(agriculture, text, "$COST_RESOURCE_AUTO roof_woodsteel", 1);
		text = modifyConstruction(agriculture, text, "$STORAGE_IMPORT RESOURCE_TRANSPORT_COVERED", 90);
		text = modifyConstruction(agriculture, text, "$STORAGE_EXPORT RESOURCE_TRANSPORT_COVERED", 50);
		map(24, 1, "Food Factory");
		map(24, 2, w);
		map(24, 3, w * c1);
		map(24, 7, w * p1);
		return text;
	} else if (filename == "distillery.ini") {
		text = modifyWorkers(alcohol, text, "$WORKERS_NEEDED", 100);
		var w = tempFileOutput;
		text = modifyProduction(alcohol, text, "$PRODUCTION alcohol", 0.06);
		var p1 = tempFileOutput;
		text = modifyConsumption(alcohol, text, "$CONSUMPTION plants", 0.3);
		var c1 = tempFileOutput;
		text = modifyConstruction(alcohol, text, "$CONSUMPTION_PER_SECOND eletric", 0.17);
		text = modifyConstruction(alcohol, text, "$STORAGE_IMPORT RESOURCE_TRANSPORT_COVERED", 80);
		text = modifyConstruction(alcohol, text, "$STORAGE_IMPORT RESOURCE_TRANSPORT_COVERED", 30);
		text = modifyConstruction(alcohol, text, "$COST_RESOURCE_AUTO ground_asphalt", 1);
		text = modifyConstruction(alcohol, text, "$COST_RESOURCE_AUTO wall_brick", 0.8);
		text = modifyConstruction(alcohol, text, "$COST_RESOURCE_AUTO wall_steel", 0.2);
		map(25, 1, "Distillery");
		map(25, 2, w);
		map(25, 3, w * c1);
		map(25, 7, w * p1);
		return text;
	} else if (filename == "powerplant_coal.ini") {
		text = modifyWorkers(energy, text, "$WORKERS_NEEDED", 40); // was 20
		var w = tempFileOutput;
		text = modifyProduction(energy, text, "$PRODUCTION eletric", 25); // was 70
		var p1 = tempFileOutput;
		text = modifyConsumption(energy, text, "$CONSUMPTION coal", 0.6);
		var c1 = tempFileOutput;
		text = modifyConstructionImpl(energy, text, "$STORAGE_IMPORT_SPECIAL RESOURCE_TRANSPORT_GRAVEL", 100, "coal", 1);
		text = modifyConstruction(energy, text, "$COST_RESOURCE_AUTO ground_asphalt", 1);
		text = modifyConstruction(energy, text, "$COST_RESOURCE_AUTO wall_steel", 0.9);
		text = modifyConstruction(energy, text, "$COST_RESOURCE_AUTO wall_concrete", 0.8);
		text = modifyConstruction(energy, text, "$COST_RESOURCE_AUTO tech_steel", 0.5);
		text = modifyConstruction(energy, text, "$COST_RESOURCE_AUTO electro_steel", 0.25);
		map(14, 1, "Coal Power Plant");
		map(14, 2, w);
		map(14, 3, w * c1);
		map(14, 7, w * p1);
		return text;
	} else if (filename == "brick_factory.ini") {
		text = modifyWorkers(cm, text, "$WORKERS_NEEDED", 75);
		var w = tempFileOutput;
		text = modifyProduction(cm, text, "$PRODUCTION bricks", 0.68);
		var p1 = tempFileOutput;
		text = modifyConsumption(cm, text, "$CONSUMPTION coal", 0.45);
		var c1 = tempFileOutput;
		text = modifyConstructionImpl(cm, text, "$STORAGE_IMPORT_SPECIAL RESOURCE_TRANSPORT_GRAVEL", 75, "coal", 1);
		text = modifyConstruction(cm, text, "$STORAGE_EXPORT RESOURCE_TRANSPORT_OPEN", 70);
		text = modifyConstruction(cm, text, "$COST_RESOURCE_AUTO ground_asphalt", 1);
		text = modifyConstruction(cm, text, "$COST_RESOURCE_AUTO wall_brick", 1);
		text = modifyConstruction(cm, text, "$COST_RESOURCE_AUTO roof_woodsteel", 1);
		map(13, 1, "Brick Factory");
		map(13, 2, w);
		map(13, 3, w * c1);
		map(13, 7, w * p1);
		return text;
	} else if (filename == "cement_plant.ini") {
		text = modifyWorkers(cm, text, "$WORKERS_NEEDED", 30);
		var w = tempFileOutput;
		text = modifyProduction(cm, text, "$PRODUCTION cement", 2.7);
		var p1 = tempFileOutput;
		text = modifyConsumption(cm, text, "$CONSUMPTION coal", 0.75);
		var c1 = tempFileOutput;
		text = modifyConsumption(cm, text, "$CONSUMPTION gravel", 7);
		var c2 = tempFileOutput;
		text = modifyConstructionImpl(cm, text, "$STORAGE_IMPORT_SPECIAL RESOURCE_TRANSPORT_GRAVEL", 75, "coal", 1);
		text = modifyConstructionImpl(cm, text, "$STORAGE_IMPORT_SPECIAL RESOURCE_TRANSPORT_GRAVEL", 75, "gravel", 2);
		text = modifyConstruction(cm, text, "$STORAGE_EXPORT RESOURCE_TRANSPORT_CEMENT", 70);
		text = modifyConstruction(cm, text, "$COST_RESOURCE_AUTO ground_asphalt", 1);
		text = modifyConstruction(cm, text, "$COST_RESOURCE_AUTO wall_concrete", 1);
		text = modifyConstruction(cm, text, "$COST_RESOURCE_AUTO wall_steel", 1);
		text = modifyConstruction(cm, text, "$COST_RESOURCE_AUTO tech_steel", 1);
		map(17, 1, "Cement Plant");
		map(17, 2, w);
		map(17, 3, w * c1);
		map(17, 4, w * c2);
		map(17, 7, w * p1);
		return text;
	} else if (filename == "asphalt_plant.ini") {
		text = modifyWorkers(cm, text, "$WORKERS_NEEDED", 5);
		var w = tempFileOutput;
		text = modifyProduction(cm, text, "$PRODUCTION asphalt", 29);
		var p1 = tempFileOutput;
		text = modifyConsumption(cm, text, "$CONSUMPTION gravel", 25);
		var c1 = tempFileOutput;
		text = modifyConsumption(cm, text, "$CONSUMPTION bitumen", 4);
		var c2 = tempFileOutput;
		text = modifyConsumption(cm, text, "$CONSUMPTION eletric", 3);
		var c3 = tempFileOutput;
		text = modifyConstruction(cm, text, "$STORAGE_IMPORT RESOURCE_TRANSPORT_OIL", 15);
		text = modifyConstruction(cm, text, "$STORAGE_IMPORT RESOURCE_TRANSPORT_GRAVEL", 30);
		text = modifyConstruction(cm, text, "$STORAGE_EXPORT RESOURCE_TRANSPORT_GRAVEL", 0.5);
		text = modifyConstruction(cm, text, "$COST_RESOURCE_AUTO ground_asphalt", 1.3);
		text = modifyConstruction(cm, text, "$COST_RESOURCE_AUTO tech_steel", 1);
		map(21, 1, "Asphalt Plant");
		map(21, 2, w);
		map(21, 3, w * c1);
		map(21, 4, w * c2);
		map(21, 5, w * c3);
		map(21, 7, w * p1);
		return text;
	} else if (filename == "animal_farm.ini") {
		text = modifyWorkers(livestock, text, "$WORKERS_NEEDED", 50);
		var w = tempFileOutput;
		text = modifyProduction(livestock, text, "$PRODUCTION livestock", 0.06); // was 0.1
		var p1 = tempFileOutput;
		text = modifyConsumption(livestock, text, "$CONSUMPTION plants", 0.2);
		var c1 = tempFileOutput;
		text = modifyConstruction(livestock, text, "$STORAGE_IMPORT RESOURCE_TRANSPORT_COVERED", 75);
		text = modifyConstruction(livestock, text, "$STORAGE_EXPORT RESOURCE_TRANSPORT_LIVESTOCK", 35);
		text = modifyConstruction(livestock, text, "$COST_RESOURCE_AUTO ground_asphalt", 0.5);
		text = modifyConstruction(livestock, text, "$COST_RESOURCE_AUTO wall_brick", 0.7);
		text = modifyConstruction(livestock, text, "$COST_RESOURCE_AUTO wall_steel", 0.5);
		text = modifyConstruction(livestock, text, "$COST_RESOURCE_AUTO roof_steel", 0.5);
		map(22, 1, "Livestock Farm");
		map(22, 2, w);
		map(22, 3, w * c1);
		map(22, 7, w * p1);
		return text;
	} else if (filename == "slaughterhouse.ini") {
		text = modifyWorkers(livestock, text, "$WORKERS_NEEDED", 50);
		var w = tempFileOutput;
		text = modifyConsumption(livestock, text, "$CONSUMPTION livestock", 5);
		var c1 = tempFileOutput;
		text = modifyProduction(livestock, text, "$PRODUCTION meat", 2.5);
		var p1 = tempFileOutput;
		text = modifyConstruction(livestock, text, "$STORAGE_IMPORT RESOURCE_TRANSPORT_LIVESTOCK", 10);
		text = modifyConstruction(livestock, text, "$STORAGE_EXPORT RESOURCE_TRANSPORT_COOLER", 5);
		text = modifyConstruction(livestock, text, "$COST_RESOURCE_AUTO ground_asphalt", 1);
		text = modifyConstruction(livestock, text, "$COST_RESOURCE_AUTO wall_brick", 0.2);
		text = modifyConstruction(livestock, text, "$COST_RESOURCE_AUTO wall_panels", 0.6);
		text = modifyConstruction(livestock, text, "$COST_RESOURCE_AUTO roof_steel", 1);
		map(23, 1, "Slaughterhouse");
		map(23, 2, w);
		map(23, 3, w * c1);
		map(23, 7, w * p1);
		return text;
	} else if (filename == "steel_mill.ini") {
		text = modifyWorkers(acm, text, "$WORKERS_NEEDED", 500);
		var w = tempFileOutput;
		text = modifyProduction(acm, text, "$PRODUCTION steel", 0.08);
		var p1 = tempFileOutput;
		text = modifyConsumption(acm, text, "$CONSUMPTION coal", 0.75);
		var c1 = tempFileOutput;
		text = modifyConsumption(acm, text, "$CONSUMPTION iron", 0.4);
		var c2 = tempFileOutput;
		text = modifyConstruction(acm, text, "$CONSUMPTION_PER_SECOND eletric", 0.19);
		text = modifyConstructionImpl(acm, text, "$STORAGE_IMPORT_SPECIAL RESOURCE_TRANSPORT_GRAVEL", 150, "iron", 1);
		text = modifyConstructionImpl(acm, text, "$STORAGE_IMPORT_SPECIAL RESOURCE_TRANSPORT_GRAVEL", 150, "coal", 2);
		text = modifyConstruction(acm, text, "$STORAGE_EXPORT RESOURCE_TRANSPORT_OPEN", 150);
		text = modifyConstruction(acm, text, "$COST_RESOURCE_AUTO ground_asphalt", 1);
		text = modifyConstruction(acm, text, "$COST_RESOURCE_AUTO wall_brick", 1);
		text = modifyConstruction(acm, text, "$COST_RESOURCE_AUTO wall_concrete", 1);
		text = modifyConstruction(acm, text, "$COST_RESOURCE_AUTO tech_steel", 0.7);
		map(10, 1, "Steel Mill");
		map(10, 2, w);
		map(10, 3, w * c1);
		map(10, 4, w * c2);
		map(10, 7, w * p1);
		return text;
	} else if (filename == "concrete_plant.ini") {
		text = modifyWorkers(acm, text, "$WORKERS_NEEDED", 5);
		var w = tempFileOutput;
		text = modifyProduction(acm, text, "$PRODUCTION concrete", 35);
		var p1 = tempFileOutput;
		text = modifyConsumption(acm, text, "$CONSUMPTION gravel", 27);
		var c1 = tempFileOutput;
		text = modifyConsumption(acm, text, "$CONSUMPTION cement", 6);
		var c2 = tempFileOutput;
		text = modifyConstruction(acm, text, "$CONSUMPTION_PER_SECOND eletric", 0.2);
		text = modifyConstruction(acm, text, "$STORAGE_IMPORT RESOURCE_TRANSPORT_GRAVEL", 80);
		text = modifyConstruction(acm, text, "$STORAGE_IMPORT RESOURCE_TRANSPORT_CEMENT", 40);
		text = modifyConstruction(acm, text, "$STORAGE_EXPORT RESOURCE_TRANSPORT_CONCRETE", 0.5);
		text = modifyConstruction(acm, text, "$COST_RESOURCE_AUTO ground_asphalt", 1);
		text = modifyConstruction(acm, text, "$COST_RESOURCE_AUTO wall_brick", 0.8);
		text = modifyConstruction(acm, text, "$COST_RESOURCE_AUTO wall_steel", 0.2);
		text = modifyConstruction(acm, text, "$COST_RESOURCE_AUTO tech_steel", 0.8);
		map(18, 1, "Concrete Plant");
		map(18, 2, w);
		map(18, 3, w * c1);
		map(18, 4, w * c2);
		map(18, 7, w * p1);
		return text;
	} else if (filename == "panels_factory.ini") {
		text = modifyWorkers(acm, text, "$WORKERS_NEEDED", 65);
		var w = tempFileOutput;
		text = modifyProduction(acm, text, "$PRODUCTION prefabpanels", 1.1);
		var p1 = tempFileOutput;
		text = modifyConsumption(acm, text, "$CONSUMPTION cement", 0.15);
		var c1 = tempFileOutput;
		text = modifyConsumption(acm, text, "$CONSUMPTION gravel", 1);
		var c2 = tempFileOutput;
		text = modifyConstruction(acm, text, "$CONSUMPTION_PER_SECOND eletric", 0.1);
		text = modifyConstruction(acm, text, "$STORAGE_IMPORT RESOURCE_TRANSPORT_CEMENT", 25);
		text = modifyConstruction(acm, text, "$STORAGE_IMPORT RESOURCE_TRANSPORT_GRAVEL", 70);
		text = modifyConstruction(acm, text, "$STORAGE_EXPORT RESOURCE_TRANSPORT_OPEN", 120);
		text = modifyConstruction(acm, text, "$COST_RESOURCE_AUTO ground_asphalt", 1);
		text = modifyConstruction(acm, text, "$COST_RESOURCE_AUTO wall_brick", 1);
		text = modifyConstruction(acm, text, "$COST_RESOURCE_AUTO tech_steel", 1);
		map(33, 1, "Prefab Factory");
		map(33, 2, w);
		map(33, 3, w * c1);
		map(33, 4, w * c2);
		map(33, 7, w * p1);
		return text;
	} else if (filename == "fabric_factory.ini") {
		text = modifyWorkers(clothing, text, "$WORKERS_NEEDED", 100);
		var w = tempFileOutput;
		text = modifyProduction(clothing, text, "$PRODUCTION fabric", 0.05);
		var p1 = tempFileOutput;
		text = modifyConsumption(clothing, text, "$CONSUMPTION plants", 0.2);
		var c1 = tempFileOutput;
		text = modifyConsumption(clothing, text, "$CONSUMPTION chemicals", 0.005);
		var c2 = tempFileOutput;
		text = modifyConstruction(clothing, text, "$CONSUMPTION_PER_SECOND eletric", 0.18);
		text = modifyConstruction(clothing, text, "$STORAGE_IMPORT RESOURCE_TRANSPORT_COVERED", 40);
		text = modifyConstruction(clothing, text, "$STORAGE_EXPORT RESOURCE_TRANSPORT_COVERED", 15);
		text = modifyConstruction(clothing, text, "$COST_RESOURCE_AUTO ground_asphalt", 1);
		text = modifyConstruction(clothing, text, "$COST_RESOURCE_AUTO wall_concrete", 0.1);
		text = modifyConstruction(clothing, text, "$COST_RESOURCE_AUTO wall_brick", 1);
		text = modifyConstruction(clothing, text, "$COST_RESOURCE_AUTO tech_steel", 0.1);
		map(28, 1, "Fabric Factory");
		map(28, 2, w);
		map(28, 3, w * c1);
		map(28, 4, w * c2);
		map(28, 7, w * p1);
		return text;
	} else if (filename == "clothing_factory.ini") {
		text = modifyWorkers(clothing, text, "$WORKERS_NEEDED", 80);
		var w = tempFileOutput;
		text = modifyProduction(clothing, text, "$PRODUCTION clothes", 0.01); // was 0.015
		var p1 = tempFileOutput;
		text = modifyConsumption(clothing, text, "$CONSUMPTION fabric", 0.03);
		var c1 = tempFileOutput;
		text = modifyConstruction(clothing, text, "$STORAGE_IMPORT RESOURCE_TRANSPORT_COVERED", 30);
		text = modifyConstruction(clothing, text, "$STORAGE_EXPORT RESOURCE_TRANSPORT_COVERED", 15);
		text = modifyConstruction(clothing, text, "$COST_RESOURCE_AUTO ground_asphalt", 1);
		text = modifyConstruction(clothing, text, "$COST_RESOURCE_AUTO wall_concrete", 0.7);
		text = modifyConstruction(clothing, text, "$COST_RESOURCE_AUTO wall_brick", 0.2);
		map(31, 1, "Clothing Factory");
		map(31, 2, w);
		map(31, 3, w * c1);
		map(31, 7, w * p1);
		return text;
	} else {
		console.log("Did not recognize file " + filename + ", skipping.");
	}

	return null;
}

RegExp.escape= function(s) {
    return s.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&');
};

var df0 = {
	format: function(val) {
		return val.toFixed(0);
	}
}

var df2 = {
	format: function(val) {
		return val.toFixed(2);
	}
}

var df3 = {
	format: function(val) {
		return val.toFixed(3);
	}
}
