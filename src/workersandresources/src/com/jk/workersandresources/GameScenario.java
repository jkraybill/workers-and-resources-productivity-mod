package com.jk.workersandresources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameScenario {

	private static Map<Integer, Map<Integer, String>> output = new HashMap<Integer, Map<Integer, String>>();

	//private static final String BUILDING_DIR = "F:\\Program Files (x86)\\Steam\\steamapps\\common\\SovietRepublic\\media_soviet\\buildings_types";
	//private static String BUILDING_DIR = "F:\\Dropbox\\personal\\games\\workersandresources\\mods\\scenario\\buildings_types";
	private static String BUILDING_DIR;

	public static final DecimalFormat df0 = new DecimalFormat("0");
	public static final DecimalFormat df2 = new DecimalFormat("0.00");
	public static final DecimalFormat df3 = new DecimalFormat("0.000");

	public static double MEANPRODUCTIVITY = 0.8; // mean for industrial productivity & size
	public static double PSD = 0.3; // standard deviation +/- for productivity & size
	public static double SD = 0.15; // standard deviation +/- for production multiples

	private static Map<String, String> fileCache = new HashMap<String, String>();

	private static double modifyWorkers(Industry ind, String filename, String key, double mean, String subdir) throws IOException {
		return modify(filename, key, mean * ind.size, mean * ind.size * SD, 1, df0, "", subdir);
	}

	private static double modifyProduction(Industry ind, String filename, String key, double mean, String subdir) throws IOException {
		return modify(filename, key, mean, SD * mean, ind.productivity, df3, "", subdir);
	}

	private static double modifyConsumption(Industry ind, String filename, String key, double mean, String subdir) throws IOException {
		return modify(filename, key, mean, SD * mean, 1, df3, "", subdir);
	}

	private static double modifyConstruction(Industry ind, String filename, String key, double mean, String subdir) throws IOException {
		return modifyConstruction(ind, filename, key, mean, "", subdir);
	}

	private static double modifyConstruction(Industry ind, String filename, String key, double mean, String suffix, String subdir) throws IOException {
		return modify(filename, key, mean, SD * mean, ind.productivity * ind.size, df3, suffix, subdir);
	}

	private static double modify(String filename, String key, double mean, double deviationUnit, double multiplier, DecimalFormat df, String suffix, String subdir)
			throws IOException {
		String body = fileCache.get(filename);

		if (body == null) {
			File f = new File(BUILDING_DIR + File.separator + filename + ".bak");
			if (!f.exists()) {
				Files.copy(new File(BUILDING_DIR + File.separator + filename).toPath(), new FileOutputStream(new File(BUILDING_DIR + File.separator + filename + ".bak")));
			}
			body = FileUtil.readFile(f);
		}
		//body = body.replace(key, "NEWVALNEWVAL");
		double gauss = RandomUtil.gaussian(mean, deviationUnit);
		double newval = Math.max(mean * 0.2, gauss) * multiplier; // don't accept gaussian values less than 20% of original (esp not negative ones)

		suffix = suffix.equals("") ? "" : (" " + suffix);
		body = body.replaceAll(Pattern.quote(key) + ".*", Matcher.quoteReplacement(key) + " " + df.format(newval) + suffix);

		System.out.println(key + ": " + df.format(newval) + suffix + " (" + df2.format(gauss) + " against mean " + df2.format(mean) + " sd " + df2.format(deviationUnit)
				+ " and multiplier " + df2.format(multiplier) + ")");

		File dir = new File(BUILDING_DIR + File.separator + subdir);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		FileWriter fw = new FileWriter(BUILDING_DIR + File.separator + subdir + File.separator + filename);
		fw.write(body);
		fw.close();
		fileCache.put(filename, body);
		return newval;
	}

	private static void map(int rowid, int colid, String s) {
		Map<Integer, String> row = output.get(rowid);
		if (row == null) {
			row = new HashMap<Integer, String>();
			output.put(rowid, row);
		}
		row.put(colid, s);
	}

	private static void map(int rowid, int colid, double val) {
		String s = df3.format(val);
		if (colid == 2) { // round workers
			s = df0.format(val);
		}
		map(rowid, colid, s);
	}

	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			System.out.println("Usage: GameScenario [buildings_types directory] [mean productivity multiplier] [industry standard deviation] [building standard deviation]");
			System.out.println("Example: GameScenario c:\\temp\\buildings_types 0.9 0.2 0.1");
			System.out.println("If not supplied, defaults are 0.8, 0.3, 0.15.");
			return;
		}
		BUILDING_DIR = args[0];
		System.out.println("Setting BUILDING_DIR to " + BUILDING_DIR);

		if (args.length > 1) {
			MEANPRODUCTIVITY = Double.parseDouble(args[1]);
		}
		System.out.println("Setting mean productivity multiplier to " + MEANPRODUCTIVITY);
		if (args.length > 2) {
			PSD = Double.parseDouble(args[2]);
		}
		System.out.println("Setting industry standard deviation to " + PSD);
		if (args.length > 3) {
			SD = Double.parseDouble(args[3]);
		}
		System.out.println("Setting building standard deviation to " + SD);

		List<Industry> industries = new ArrayList<Industry>();

		String nation = getNationName();
		String subdir = nation + "-" + System.currentTimeMillis();

		Industry wood = new Industry("Wood");
		industries.add(wood);

		double w = modifyWorkers(wood, "woodcutting_post.ini", "$WORKERS_NEEDED", 30, subdir);
		double p1 = modifyProduction(wood, "woodcutting_post.ini", "$PRODUCTION wood", 6.25, subdir);
		modifyConstruction(wood, "woodcutting_post.ini", "$STORAGE_EXPORT RESOURCE_TRANSPORT_OPEN", 50, subdir);
		modifyConstruction(wood, "woodcutting_post.ini", "$COST_RESOURCE_AUTO ground_asphalt", 1, subdir);
		modifyConstruction(wood, "woodcutting_post.ini", "$COST_RESOURCE_AUTO wall_wood", 0.8, subdir);
		modifyConstruction(wood, "woodcutting_post.ini", "$COST_RESOURCE_AUTO roof_woodsteel", 0.8, subdir);
		map(5, 1, "Woodcutting Post");
		map(5, 2, w);
		map(5, 7, w * p1);

		w = modifyWorkers(wood, "sawmill.ini", "$WORKERS_NEEDED", 20, subdir);
		p1 = modifyProduction(wood, "sawmill.ini", "$PRODUCTION boards", 7, subdir);
		double c1 = modifyConsumption(wood, "sawmill.ini", "$CONSUMPTION wood", 9, subdir);
		modifyConstruction(wood, "sawmill.ini", "$CONSUMPTION_PER_SECOND eletric", 0.027, subdir);
		modifyConstruction(wood, "sawmill.ini", "$STORAGE_IMPORT RESOURCE_TRANSPORT_OPEN", 60, subdir);
		modifyConstruction(wood, "sawmill.ini", "$STORAGE_EXPORT RESOURCE_TRANSPORT_OPEN", 50, subdir);
		modifyConstruction(wood, "sawmill.ini", "$COST_RESOURCE_AUTO ground_asphalt", 1, subdir);
		modifyConstruction(wood, "sawmill.ini", "$COST_RESOURCE_AUTO wall_brick", 1, subdir);
		modifyConstruction(wood, "sawmill.ini", "$COST_RESOURCE_AUTO roof_woodsteel", 1, subdir);
		map(6, 1, "Sawmill");
		map(6, 2, w);
		map(6, 3, w * c1);
		map(6, 7, w * p1);

		Industry iron = new Industry("Iron");
		industries.add(iron);
		p1 = modifyProduction(iron, "iron_mine.ini", "$PRODUCTION rawiron", 4, subdir);
		modifyConstruction(iron, "iron_mine.ini", "$STORAGE_EXPORT RESOURCE_TRANSPORT_GRAVEL", 20, subdir);
		w = modifyWorkers(iron, "iron_mine.ini", "$WORKERS_NEEDED", 250, subdir);
		modifyConstruction(iron, "iron_mine.ini", "$COST_RESOURCE_AUTO ground_asphalt", 1, subdir);
		modifyConstruction(iron, "iron_mine.ini", "$COST_RESOURCE_AUTO wall_concrete", 1, subdir);
		modifyConstruction(iron, "iron_mine.ini", "$COST_RESOURCE_AUTO tech_steel", 0.8, subdir);
		modifyConstruction(iron, "iron_mine.ini", "$COST_RESOURCE workers", 3050, subdir);
		modifyConstruction(iron, "iron_mine.ini", "$COST_RESOURCE boards", 75, subdir);
		modifyConstruction(iron, "iron_mine.ini", "$COST_RESOURCE concrete", 180, subdir);
		modifyConstruction(iron, "iron_mine.ini", "$COST_RESOURCE steel", 45, subdir);
		map(7, 1, "Iron Mine");
		map(7, 2, w);
		map(7, 7, w * p1);

		w = modifyWorkers(iron, "iron_processing.ini", "$WORKERS_NEEDED", 15, subdir);
		p1 = modifyProduction(iron, "iron_processing.ini", "$PRODUCTION iron", 7, subdir);
		c1 = modifyConsumption(iron, "iron_processing.ini", "$CONSUMPTION rawiron", 15, subdir);
		modifyConstruction(iron, "iron_processing.ini", "$CONSUMPTION_PER_SECOND eletric", 0.28, subdir);
		modifyConstruction(iron, "iron_processing.ini", "$STORAGE_IMPORT RESOURCE_TRANSPORT_GRAVEL", 25, subdir);
		modifyConstruction(iron, "iron_processing.ini", "$STORAGE_EXPORT RESOURCE_TRANSPORT_GRAVEL", 25, subdir);
		modifyConstruction(iron, "iron_processing.ini", "$COST_RESOURCE_AUTO ground_asphalt", 1, subdir);
		modifyConstruction(iron, "iron_processing.ini", "$COST_RESOURCE_AUTO wall_concrete", 1, subdir);
		modifyConstruction(iron, "iron_processing.ini", "$COST_RESOURCE_AUTO wall_steel", 1, subdir);
		modifyConstruction(iron, "iron_processing.ini", "$COST_RESOURCE_AUTO tech_steel", 1, subdir);
		map(8, 1, "Iron Processing");
		map(8, 2, w);
		map(8, 3, w * c1);
		map(8, 7, w * p1);

		Industry coal = new Industry("Coal");
		industries.add(coal);
		p1 = modifyProduction(coal, "coal_mine.ini", "$PRODUCTION rawcoal", 4.2, subdir);
		modifyConstruction(coal, "coal_mine.ini", "$STORAGE_EXPORT RESOURCE_TRANSPORT_GRAVEL", 20, subdir);
		w = modifyWorkers(coal, "coal_mine.ini", "$WORKERS_NEEDED", 220, subdir);
		modifyConstruction(coal, "coal_mine.ini", "$COST_RESOURCE_AUTO ground_asphalt", 1, subdir);
		modifyConstruction(coal, "coal_mine.ini", "$COST_RESOURCE_AUTO wall_concrete", 1, subdir);
		modifyConstruction(coal, "coal_mine.ini", "$COST_RESOURCE_AUTO tech_steel", 0.8, subdir);
		modifyConstruction(coal, "coal_mine.ini", "$COST_RESOURCE workers", 3000, subdir);
		modifyConstruction(coal, "coal_mine.ini", "$COST_RESOURCE boards", 75, subdir);
		modifyConstruction(coal, "coal_mine.ini", "$COST_RESOURCE concrete", 180, subdir);
		modifyConstruction(coal, "coal_mine.ini", "$COST_RESOURCE steel", 45, subdir);
		map(10, 1, "Coal Mine");
		map(10, 2, w);
		map(10, 7, w * p1);

		w = modifyWorkers(coal, "coal_processing.ini", "$WORKERS_NEEDED", 30, subdir); // was 15
		p1 = modifyProduction(coal, "coal_processing.ini", "$PRODUCTION coal", 2.5, subdir); // was 8
		c1 = modifyConsumption(coal, "coal_processing.ini", "$CONSUMPTION rawcoal", 7, subdir);
		modifyConstruction(coal, "coal_processing.ini", "$CONSUMPTION_PER_SECOND eletric", 0.25, subdir);
		modifyConstruction(coal, "coal_processing.ini", "$STORAGE_IMPORT RESOURCE_TRANSPORT_GRAVEL", 25, subdir);
		modifyConstruction(coal, "coal_processing.ini", "$STORAGE_EXPORT RESOURCE_TRANSPORT_GRAVEL", 25, subdir);
		modifyConstruction(coal, "coal_processing.ini", "$COST_RESOURCE_AUTO ground_asphalt", 1, subdir);
		modifyConstruction(coal, "coal_processing.ini", "$COST_RESOURCE_AUTO wall_concrete", 1, subdir);
		modifyConstruction(coal, "coal_processing.ini", "$COST_RESOURCE_AUTO wall_steel", 1, subdir);
		modifyConstruction(coal, "coal_processing.ini", "$COST_RESOURCE_AUTO tech_steel", 1, subdir);
		map(11, 1, "Coal Processing");
		map(11, 2, w);
		map(11, 3, w * c1);
		map(11, 7, w * p1);

		Industry gravel = new Industry("Gravel");
		industries.add(gravel);
		w = modifyWorkers(gravel, "gravelmine.ini", "$WORKERS_NEEDED", 40, subdir);
		p1 = modifyProduction(gravel, "gravelmine.ini", "$PRODUCTION rawgravel", 3.5, subdir);
		modifyConstruction(gravel, "gravelmine.ini", "$STORAGE_EXPORT RESOURCE_TRANSPORT_GRAVEL", 0.5, subdir);
		modifyConstruction(gravel, "gravelmine.ini", "$COST_RESOURCE_AUTO ground_asphalt", 3, subdir);
		modifyConstruction(gravel, "gravelmine.ini", "$COST_RESOURCE_AUTO wall_brick", 2.8, subdir);
		modifyConstruction(gravel, "gravelmine.ini", "$COST_RESOURCE_AUTO roof_woodsteel", 2.8, subdir);
		map(14, 1, "Quarry");
		map(14, 2, w);
		map(14, 7, w * p1);

		w = modifyWorkers(gravel, "gravel_processing.ini", "$WORKERS_NEEDED", 15, subdir);
		p1 = modifyProduction(gravel, "gravel_processing.ini", "$PRODUCTION gravel", 5.5, subdir);
		c1 = modifyConsumption(gravel, "gravel_processing.ini", "$CONSUMPTION rawgravel", 8, subdir);
		modifyConstruction(gravel, "gravel_processing.ini", "$CONSUMPTION_PER_SECOND eletric", 0.4, subdir);
		modifyConstruction(gravel, "gravel_processing.ini", "$COST_RESOURCE_AUTO ground_asphalt", 1, subdir);
		modifyConstruction(gravel, "gravel_processing.ini", "$COST_RESOURCE_AUTO wall_concrete", 0.8, subdir);
		modifyConstruction(gravel, "gravel_processing.ini", "$COST_RESOURCE_AUTO wall_steel", 0.35, subdir);
		modifyConstruction(gravel, "gravel_processing.ini", "$COST_RESOURCE_AUTO tech_steel", 0.35, subdir);
		modifyConstruction(gravel, "gravel_processing.ini", "$STORAGE_IMPORT RESOURCE_TRANSPORT_GRAVEL", 10, subdir);
		modifyConstruction(gravel, "gravel_processing.ini", "$STORAGE_EXPORT RESOURCE_TRANSPORT_GRAVEL", 145, subdir);
		map(15, 1, "Gravel Processing");
		map(15, 2, w);
		map(15, 3, w * c1);
		map(15, 7, w * p1);

		Industry oil = new Industry("Oil");
		industries.add(oil);
		p1 = modifyProduction(oil, "oil_mine.ini", "$PRODUCTION oil", 3.5, subdir); // was 7
		modifyConstruction(oil, "oil_mine.ini", "$CONSUMPTION_PER_SECOND eletric", 0.065, subdir);
		modifyConstruction(oil, "oil_mine.ini", "$STORAGE_EXPORT RESOURCE_TRANSPORT_OIL", 15, subdir);
		modifyConstruction(oil, "oil_mine.ini", "$COST_RESOURCE_AUTO ground_asphalt", 1, subdir);
		modifyConstruction(oil, "oil_mine.ini", "$COST_RESOURCE_AUTO tech_steel", 2, subdir);
		map(18, 1, "Oil Rig");
		map(18, 2, 1); // "1" worker
		map(18, 7, p1);

		w = modifyWorkers(oil, "oil_rafinery.ini", "$WORKERS_NEEDED", 500, subdir);
		p1 = modifyProduction(oil, "oil_rafinery.ini", "$PRODUCTION fuel", 0.2, subdir); // was 0.25
		double p2 = modifyProduction(oil, "oil_rafinery.ini", "$PRODUCTION bitumen", 0.12, subdir); // was 0.15
		c1 = modifyConsumption(oil, "oil_rafinery.ini", "$CONSUMPTION oil", 0.5, subdir);
		modifyConstruction(oil, "oil_rafinery.ini", "$CONSUMPTION_PER_SECOND eletric", 0.21, subdir);
		modifyConstruction(oil, "oil_rafinery.ini", "$STORAGE_IMPORT RESOURCE_TRANSPORT_OIL", 450, subdir);
		modifyConstruction(oil, "oil_rafinery.ini", "$STORAGE_EXPORT_SPECIAL RESOURCE_TRANSPORT_OIL", 340, "fuel", subdir);
		modifyConstruction(oil, "oil_rafinery.ini", "$STORAGE_EXPORT_SPECIAL RESOURCE_TRANSPORT_OIL", 250, "bitumen", subdir);
		modifyConstruction(oil, "oil_rafinery.ini", "$COST_RESOURCE_AUTO ground_asphalt", 1, subdir);
		modifyConstruction(oil, "oil_rafinery.ini", "$COST_RESOURCE_AUTO tech_steel", 0.8, subdir);
		map(19, 1, "Oil Refinery");
		map(19, 2, w);
		map(19, 3, w * c1);
		map(19, 7, w * p1);
		map(19, 8, w * p2);

		Industry agriculture = new Industry("Agriculture");
		industries.add(agriculture);
		modifyProduction(agriculture, "farm.ini", "$PRODUCTION plants", 0.01, subdir);
		modifyConstruction(agriculture, "farm.ini", "$STORAGE_EXPORT RESOURCE_TRANSPORT_COVERED", 170, subdir);
		modifyConstruction(agriculture, "farm.ini", "$COST_RESOURCE_AUTO ground_asphalt", 0.6, subdir);
		modifyConstruction(agriculture, "farm.ini", "$COST_RESOURCE_AUTO wall_steel", 0.6, subdir);
		modifyConstruction(agriculture, "farm.ini", "$COST_RESOURCE_AUTO tech_steel", 0.5, subdir);

		w = modifyWorkers(agriculture, "food_factory.ini", "$WORKERS_NEEDED", 170, subdir);
		p1 = modifyProduction(agriculture, "food_factory.ini", "$PRODUCTION food", 0.12, subdir);
		c1 = modifyConsumption(agriculture, "food_factory.ini", "$CONSUMPTION plants", 0.25, subdir);
		modifyConstruction(agriculture, "food_factory.ini", "$COST_RESOURCE_AUTO ground_asphalt", 1, subdir);
		modifyConstruction(agriculture, "food_factory.ini", "$COST_RESOURCE_AUTO wall_brick", 0.8, subdir);
		modifyConstruction(agriculture, "food_factory.ini", "$COST_RESOURCE_AUTO wall_steel", 0.2, subdir);
		modifyConstruction(agriculture, "food_factory.ini", "$COST_RESOURCE_AUTO roof_woodsteel", 1, subdir);
		modifyConstruction(agriculture, "food_factory.ini", "$STORAGE_IMPORT RESOURCE_TRANSPORT_COVERED", 90, subdir);
		modifyConstruction(agriculture, "food_factory.ini", "$STORAGE_EXPORT RESOURCE_TRANSPORT_COVERED", 50, subdir);
		map(23, 1, "Food Factory");
		map(23, 2, w);
		map(23, 3, w * c1);
		map(23, 7, w * p1);

		Industry alcohol = new Industry("Alcohol");
		industries.add(alcohol);
		w = modifyWorkers(alcohol, "distillery.ini", "$WORKERS_NEEDED", 100, subdir);
		p1 = modifyProduction(alcohol, "distillery.ini", "$PRODUCTION alcohol", 0.06, subdir);
		c1 = modifyConsumption(alcohol, "distillery.ini", "$CONSUMPTION plants", 0.3, subdir);
		modifyConstruction(alcohol, "distillery.ini", "$CONSUMPTION_PER_SECOND eletric", 0.17, subdir);
		modifyConstruction(alcohol, "distillery.ini", "$STORAGE_IMPORT RESOURCE_TRANSPORT_COVERED", 80, subdir);
		modifyConstruction(alcohol, "distillery.ini", "$STORAGE_IMPORT RESOURCE_TRANSPORT_COVERED", 30, subdir);
		modifyConstruction(alcohol, "distillery.ini", "$COST_RESOURCE_AUTO ground_asphalt", 1, subdir);
		modifyConstruction(alcohol, "distillery.ini", "$COST_RESOURCE_AUTO wall_brick", 0.8, subdir);
		modifyConstruction(alcohol, "distillery.ini", "$COST_RESOURCE_AUTO wall_steel", 0.2, subdir);
		map(24, 1, "Distillery");
		map(24, 2, w);
		map(24, 3, w * c1);
		map(24, 7, w * p1);

		Industry energy = new Industry("Energy");
		industries.add(energy);
		w = modifyWorkers(energy, "powerplant_coal.ini", "$WORKERS_NEEDED", 40, subdir); // was 20
		p1 = modifyProduction(energy, "powerplant_coal.ini", "$PRODUCTION eletric", 25, subdir); // was 70
		c1 = modifyConsumption(energy, "powerplant_coal.ini", "$CONSUMPTION coal", 0.6, subdir);
		modifyConstruction(energy, "powerplant_coal.ini", "$STORAGE_IMPORT_SPECIAL RESOURCE_TRANSPORT_GRAVEL", 100, "coal", subdir);
		modifyConstruction(energy, "powerplant_coal.ini", "$COST_RESOURCE_AUTO ground_asphalt", 1, subdir);
		modifyConstruction(energy, "powerplant_coal.ini", "$COST_RESOURCE_AUTO wall_steel", 0.9, subdir);
		modifyConstruction(energy, "powerplant_coal.ini", "$COST_RESOURCE_AUTO wall_concrete", 0.8, subdir);
		modifyConstruction(energy, "powerplant_coal.ini", "$COST_RESOURCE_AUTO tech_steel", 0.5, subdir);
		modifyConstruction(energy, "powerplant_coal.ini", "$COST_RESOURCE_AUTO electro_steel", 0.25, subdir);
		map(13, 1, "Coal Power Plant");
		map(13, 2, w);
		map(13, 3, w * c1);
		map(13, 7, w * p1);

		Industry cm = new Industry("Construction Materials");
		industries.add(cm);
		w = modifyWorkers(cm, "brick_factory.ini", "$WORKERS_NEEDED", 75, subdir);
		p1 = modifyProduction(cm, "brick_factory.ini", "$PRODUCTION bricks", 0.68, subdir);
		c1 = modifyConsumption(cm, "brick_factory.ini", "$CONSUMPTION coal", 0.45, subdir);
		modifyConstruction(cm, "brick_factory.ini", "$STORAGE_IMPORT_SPECIAL RESOURCE_TRANSPORT_GRAVEL", 75, "coal", subdir);
		modifyConstruction(cm, "brick_factory.ini", "$STORAGE_EXPORT RESOURCE_TRANSPORT_OPEN", 70, subdir);
		modifyConstruction(cm, "brick_factory.ini", "$COST_RESOURCE_AUTO ground_asphalt", 1, subdir);
		modifyConstruction(cm, "brick_factory.ini", "$COST_RESOURCE_AUTO wall_brick", 1, subdir);
		modifyConstruction(cm, "brick_factory.ini", "$COST_RESOURCE_AUTO roof_woodsteel", 1, subdir);
		map(12, 1, "Brick Factory");
		map(12, 2, w);
		map(12, 3, w * c1);
		map(12, 7, w * p1);

		w = modifyWorkers(cm, "cement_plant.ini", "$WORKERS_NEEDED", 30, subdir);
		p1 = modifyProduction(cm, "cement_plant.ini", "$PRODUCTION cement", 2.7, subdir);
		c1 = modifyConsumption(cm, "cement_plant.ini", "$CONSUMPTION coal", 0.75, subdir);
		double c2 = modifyConsumption(cm, "cement_plant.ini", "$CONSUMPTION gravel", 7, subdir);
		modifyConstruction(cm, "cement_plant.ini", "$STORAGE_IMPORT_SPECIAL RESOURCE_TRANSPORT_GRAVEL", 75, "coal", subdir);
		modifyConstruction(cm, "cement_plant.ini", "$STORAGE_IMPORT_SPECIAL RESOURCE_TRANSPORT_GRAVEL", 75, "gravel", subdir);
		modifyConstruction(cm, "cement_plant.ini", "$STORAGE_EXPORT RESOURCE_TRANSPORT_CEMENT", 70, subdir);
		modifyConstruction(cm, "cement_plant.ini", "$COST_RESOURCE_AUTO ground_asphalt", 1, subdir);
		modifyConstruction(cm, "cement_plant.ini", "$COST_RESOURCE_AUTO wall_concrete", 1, subdir);
		modifyConstruction(cm, "cement_plant.ini", "$COST_RESOURCE_AUTO wall_steel", 1, subdir);
		modifyConstruction(cm, "cement_plant.ini", "$COST_RESOURCE_AUTO tech_steel", 1, subdir);
		map(16, 1, "Cement Plant");
		map(16, 2, w);
		map(16, 3, w * c1);
		map(16, 4, w * c2);
		map(16, 7, w * p1);

		w = modifyWorkers(cm, "asphalt_plant.ini", "$WORKERS_NEEDED", 5, subdir);
		p1 = modifyProduction(cm, "asphalt_plant.ini", "$PRODUCTION asphalt", 29, subdir);
		c1 = modifyConsumption(cm, "asphalt_plant.ini", "$CONSUMPTION gravel", 25, subdir);
		c2 = modifyConsumption(cm, "asphalt_plant.ini", "$CONSUMPTION bitumen", 4, subdir);
		double c3 = modifyConsumption(cm, "asphalt_plant.ini", "$CONSUMPTION eletric", 3, subdir);
		modifyConstruction(cm, "asphalt_plant.ini", "$STORAGE_IMPORT RESOURCE_TRANSPORT_OIL", 15, subdir);
		modifyConstruction(cm, "asphalt_plant.ini", "$STORAGE_IMPORT RESOURCE_TRANSPORT_GRAVEL", 30, subdir);
		modifyConstruction(cm, "asphalt_plant.ini", "$STORAGE_EXPORT RESOURCE_TRANSPORT_GRAVEL", 0.5, subdir);
		modifyConstruction(cm, "asphalt_plant.ini", "$COST_RESOURCE_AUTO ground_asphalt", 1.3, subdir);
		modifyConstruction(cm, "asphalt_plant.ini", "$COST_RESOURCE_AUTO tech_steel", 1, subdir);
		map(20, 1, "Asphalt Plant");
		map(20, 2, w);
		map(20, 3, w * c1);
		map(20, 4, w * c2);
		map(20, 5, w * c3);
		map(20, 7, w * p1);

		Industry livestock = new Industry("Livestock");
		industries.add(livestock);
		w = modifyWorkers(livestock, "animal_farm.ini", "$WORKERS_NEEDED", 50, subdir);
		p1 = modifyProduction(livestock, "animal_farm.ini", "$PRODUCTION livestock", 0.06, subdir); // was 0.1
		c1 = modifyConsumption(livestock, "animal_farm.ini", "$CONSUMPTION plants", 0.2, subdir);
		modifyConstruction(livestock, "animal_farm.ini", "$STORAGE_IMPORT RESOURCE_TRANSPORT_COVERED", 75, subdir);
		modifyConstruction(livestock, "animal_farm.ini", "$STORAGE_EXPORT RESOURCE_TRANSPORT_LIVESTOCK", 35, subdir);
		modifyConstruction(livestock, "animal_farm.ini", "$COST_RESOURCE_AUTO ground_asphalt", 0.5, subdir);
		modifyConstruction(livestock, "animal_farm.ini", "$COST_RESOURCE_AUTO wall_brick", 0.7, subdir);
		modifyConstruction(livestock, "animal_farm.ini", "$COST_RESOURCE_AUTO wall_steel", 0.5, subdir);
		modifyConstruction(livestock, "animal_farm.ini", "$COST_RESOURCE_AUTO roof_steel", 0.5, subdir);
		map(21, 1, "Livestock Farm");
		map(21, 2, w);
		map(21, 3, w * c1);
		map(21, 7, w * p1);

		w = modifyWorkers(livestock, "slaughterhouse.ini", "$WORKERS_NEEDED", 50, subdir);
		c1 = modifyConsumption(livestock, "slaughterhouse.ini", "$CONSUMPTION livestock", 5, subdir);
		p1 = modifyProduction(livestock, "slaughterhouse.ini", "$PRODUCTION meat", 2.5, subdir);
		modifyConstruction(livestock, "slaughterhouse.ini", "$STORAGE_IMPORT RESOURCE_TRANSPORT_LIVESTOCK", 10, subdir);
		modifyConstruction(livestock, "slaughterhouse.ini", "$STORAGE_EXPORT RESOURCE_TRANSPORT_COOLER", 5, subdir);
		modifyConstruction(livestock, "slaughterhouse.ini", "$COST_RESOURCE_AUTO ground_asphalt", 1, subdir);
		modifyConstruction(livestock, "slaughterhouse.ini", "$COST_RESOURCE_AUTO wall_brick", 0.2, subdir);
		modifyConstruction(livestock, "slaughterhouse.ini", "$COST_RESOURCE_AUTO wall_panels", 0.6, subdir);
		modifyConstruction(livestock, "slaughterhouse.ini", "$COST_RESOURCE_AUTO roof_steel", 1, subdir);
		map(22, 1, "Slaughterhouse");
		map(22, 2, w);
		map(22, 3, w * c1);
		map(22, 7, w * p1);

		Industry acm = new Industry("Advanced Construction Materials");
		industries.add(acm);

		w = modifyWorkers(acm, "steel_mill.ini", "$WORKERS_NEEDED", 500, subdir);
		p1 = modifyProduction(acm, "steel_mill.ini", "$PRODUCTION steel", 0.08, subdir);
		c1 = modifyConsumption(acm, "steel_mill.ini", "$CONSUMPTION coal", 0.75, subdir);
		c2 = modifyConsumption(acm, "steel_mill.ini", "$CONSUMPTION iron", 0.4, subdir);
		modifyConstruction(acm, "steel_mill.ini", "$CONSUMPTION_PER_SECOND eletric", 0.19, subdir);
		modifyConstruction(acm, "steel_mill.ini", "$STORAGE_IMPORT_SPECIAL RESOURCE_TRANSPORT_GRAVEL", 150, "iron", subdir);
		modifyConstruction(acm, "steel_mill.ini", "$STORAGE_IMPORT_SPECIAL RESOURCE_TRANSPORT_GRAVEL", 150, "coal", subdir);
		modifyConstruction(acm, "steel_mill.ini", "$STORAGE_EXPORT RESOURCE_TRANSPORT_OPEN", 150, subdir);
		modifyConstruction(acm, "steel_mill.ini", "$COST_RESOURCE_AUTO ground_asphalt", 1, subdir);
		modifyConstruction(acm, "steel_mill.ini", "$COST_RESOURCE_AUTO wall_brick", 1, subdir);
		modifyConstruction(acm, "steel_mill.ini", "$COST_RESOURCE_AUTO wall_concrete", 1, subdir);
		modifyConstruction(acm, "steel_mill.ini", "$COST_RESOURCE_AUTO tech_steel", 0.7, subdir);
		map(9, 1, "Steel Mill");
		map(9, 2, w);
		map(9, 3, w * c1);
		map(9, 4, w * c2);
		map(9, 7, w * p1);

		w = modifyWorkers(acm, "concrete_plant.ini", "$WORKERS_NEEDED", 5, subdir);
		p1 = modifyProduction(acm, "concrete_plant.ini", "$PRODUCTION concrete", 35, subdir);
		c1 = modifyConsumption(acm, "concrete_plant.ini", "$CONSUMPTION gravel", 27, subdir);
		c2 = modifyConsumption(acm, "concrete_plant.ini", "$CONSUMPTION cement", 6, subdir);
		modifyConstruction(acm, "concrete_plant.ini", "$CONSUMPTION_PER_SECOND eletric", 0.2, subdir);
		modifyConstruction(acm, "concrete_plant.ini", "$STORAGE_IMPORT RESOURCE_TRANSPORT_GRAVEL", 80, subdir);
		modifyConstruction(acm, "concrete_plant.ini", "$STORAGE_IMPORT RESOURCE_TRANSPORT_CEMENT", 40, subdir);
		modifyConstruction(acm, "concrete_plant.ini", "$STORAGE_EXPORT RESOURCE_TRANSPORT_CONCRETE", 0.5, subdir);
		modifyConstruction(acm, "concrete_plant.ini", "$COST_RESOURCE_AUTO ground_asphalt", 1, subdir);
		modifyConstruction(acm, "concrete_plant.ini", "$COST_RESOURCE_AUTO wall_brick", 0.8, subdir);
		modifyConstruction(acm, "concrete_plant.ini", "$COST_RESOURCE_AUTO wall_steel", 0.2, subdir);
		modifyConstruction(acm, "concrete_plant.ini", "$COST_RESOURCE_AUTO tech_steel", 0.8, subdir);
		map(17, 1, "Concrete Plant");
		map(17, 2, w);
		map(17, 3, w * c1);
		map(17, 4, w * c2);
		map(17, 7, w * p1);

		w = modifyWorkers(acm, "panels_factory.ini", "$WORKERS_NEEDED", 65, subdir);
		p1 = modifyProduction(acm, "panels_factory.ini", "$PRODUCTION prefabpanels", 1.1, subdir);
		c1 = modifyConsumption(acm, "panels_factory.ini", "$CONSUMPTION cement", 0.15, subdir);
		c2 = modifyConsumption(acm, "panels_factory.ini", "$CONSUMPTION gravel", 1, subdir);
		modifyConstruction(acm, "panels_factory.ini", "$CONSUMPTION_PER_SECOND eletric", 0.1, subdir);
		modifyConstruction(acm, "panels_factory.ini", "$STORAGE_IMPORT RESOURCE_TRANSPORT_CEMENT", 25, subdir);
		modifyConstruction(acm, "panels_factory.ini", "$STORAGE_IMPORT RESOURCE_TRANSPORT_GRAVEL", 70, subdir);
		modifyConstruction(acm, "panels_factory.ini", "$STORAGE_EXPORT RESOURCE_TRANSPORT_OPEN", 120, subdir);
		modifyConstruction(acm, "panels_factory.ini", "$COST_RESOURCE_AUTO ground_asphalt", 1, subdir);
		modifyConstruction(acm, "panels_factory.ini", "$COST_RESOURCE_AUTO wall_brick", 1, subdir);
		modifyConstruction(acm, "panels_factory.ini", "$COST_RESOURCE_AUTO tech_steel", 1, subdir);
		map(32, 1, "Prefab Factory");
		map(32, 2, w);
		map(32, 3, w * c1);
		map(32, 4, w * c2);
		map(32, 7, w * p1);

		Industry clothing = new Industry("Clothing");
		industries.add(clothing);

		w = modifyWorkers(clothing, "fabric_factory.ini", "$WORKERS_NEEDED", 100, subdir);
		p1 = modifyProduction(clothing, "fabric_factory.ini", "$PRODUCTION fabric", 0.05, subdir);
		c1 = modifyConsumption(clothing, "fabric_factory.ini", "$CONSUMPTION plants", 0.2, subdir);
		c2 = modifyConsumption(clothing, "fabric_factory.ini", "$CONSUMPTION chemicals", 0.005, subdir);
		modifyConstruction(clothing, "fabric_factory.ini", "$CONSUMPTION_PER_SECOND eletric", 0.18, subdir);
		modifyConstruction(clothing, "fabric_factory.ini", "$STORAGE_IMPORT RESOURCE_TRANSPORT_COVERED", 40, subdir);
		modifyConstruction(clothing, "fabric_factory.ini", "$STORAGE_EXPORT RESOURCE_TRANSPORT_COVERED", 15, subdir);
		modifyConstruction(clothing, "fabric_factory.ini", "$COST_RESOURCE_AUTO ground_asphalt", 1, subdir);
		modifyConstruction(clothing, "fabric_factory.ini", "$COST_RESOURCE_AUTO wall_concrete", 0.1, subdir);
		modifyConstruction(clothing, "fabric_factory.ini", "$COST_RESOURCE_AUTO wall_brick", 1, subdir);
		modifyConstruction(clothing, "fabric_factory.ini", "$COST_RESOURCE_AUTO tech_steel", 0.1, subdir);
		map(27, 1, "Fabric Factory");
		map(27, 2, w);
		map(27, 3, w * c1);
		map(27, 4, w * c2);
		map(27, 7, w * p1);

		w = modifyWorkers(clothing, "clothing_factory.ini", "$WORKERS_NEEDED", 80, subdir);
		p1 = modifyProduction(clothing, "clothing_factory.ini", "$PRODUCTION clothes", 0.01, subdir); // was 0.015
		c1 = modifyConsumption(clothing, "clothing_factory.ini", "$CONSUMPTION fabric", 0.03, subdir);
		modifyConstruction(clothing, "clothing_factory.ini", "$STORAGE_IMPORT RESOURCE_TRANSPORT_COVERED", 30, subdir);
		modifyConstruction(clothing, "clothing_factory.ini", "$STORAGE_EXPORT RESOURCE_TRANSPORT_COVERED", 15, subdir);
		modifyConstruction(clothing, "clothing_factory.ini", "$COST_RESOURCE_AUTO ground_asphalt", 1, subdir);
		modifyConstruction(clothing, "clothing_factory.ini", "$COST_RESOURCE_AUTO wall_concrete", 0.7, subdir);
		modifyConstruction(clothing, "clothing_factory.ini", "$COST_RESOURCE_AUTO wall_brick", 0.2, subdir);
		map(30, 1, "Clothing Factory");
		map(30, 2, w);
		map(30, 3, w * c1);
		map(30, 7, w * p1);

		Comparator<Industry> compareBySize = (Industry i1, Industry i2) -> new Double(i1.size).compareTo(i2.size);
		Comparator<Industry> compareByFame = (Industry i1, Industry i2) -> new Double(i1.fame).compareTo(i2.fame);

		System.out.println("Greetings from the great nation of " + nation + "!");
		System.out.println("\nOur people are most productive in:");

		Collections.sort(industries);
		Collections.reverse(industries);
		for (Industry i : industries) {
			System.out.println(i.name + " (" + df2.format(i.productivity) + ")");
		}

		System.out.println("\nOur largest industries are:");
		Collections.sort(industries, compareBySize);
		Collections.reverse(industries);
		for (Industry i : industries) {
			System.out.println(i.name + " (" + df2.format(i.size) + ")");
		}

		System.out.println("\nOur people are most famous for:");
		Collections.sort(industries, compareByFame);
		Collections.reverse(industries);
		for (Industry i : industries) {
			System.out.println(i.name + " (" + df2.format(i.fame) + ")");
		}

		System.out.print("\n");
		FileWriter fw = new FileWriter(BUILDING_DIR + File.separator + subdir + File.separator + "data.tsv");
		for (int r = 5; r < 33; r++) {
			for (int c = 1; c < 9; c++) {
				Map<Integer, String> row = output.get(r);
				if (row == null) {
					fw.write("\t");
				} else {
					String val = row.get(c);
					if (val == null) {
						fw.write("\t");
					} else {
						fw.write(val + "\t");
					}
				}
			}
			fw.write("\n");
		}
		fw.close();
		System.out.println("Config files are in " + BUILDING_DIR + File.separator + subdir
				+ " -- copy over the files in your buildings_types game directory (MAKE SURE TO BACK THEM UP FIRST!).");
		System.out.println("Data for all the industries is in " + BUILDING_DIR + File.separator + subdir + File.separator + "data.tsv");
	}

	private static String[] NAME1 = new String[] { "Uzbek", "Turk", "Czech", "Taj", "Al", "Af", "Ar", "Uk", "Ug", "Bul", "Ro", "Mol", "Es", "Azer" };
	private static String[] NAME2 = new String[] { "", "men", "ik", "ban", "ghan", "slav", "oslav", "oslov", "stotz", "dov", "on", "v" };
	private static String[] NAME3 = new String[] { "ia", "istan", "akia", "a" };

	private static String getNationName() {
		return RandomUtil.get(NAME1) + RandomUtil.get(NAME2) + RandomUtil.get(NAME3);

	}

	private static class Industry implements Comparable<Industry> {
		String name;
		double size;
		double productivity;
		double fame;

		Industry(String label) {
			name = label;
			size = Math.max(0.2, RandomUtil.gaussian(MEANPRODUCTIVITY, PSD));
			productivity = Math.max(0.2, RandomUtil.gaussian(MEANPRODUCTIVITY, PSD));
			fame = size * productivity;
			System.out.println("---\n" + name + " size " + df2.format(size));
			System.out.println(name + " cost and productivity " + df2.format(productivity));
		}

		@Override
		public int compareTo(Industry i) {
			return new Double(productivity).compareTo(i.productivity);
		}
	}
}
