package ch.fmi;

import com.google.common.primitives.Doubles;

import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.detection.DetectorKeys;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.extra.spotanalyzer.SpotMultiChannelIntensityAnalyzerFactory;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.features.spot.SpotContrastAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotRadiusEstimatorFactory;
import fiji.plugin.trackmate.features.track.TrackDurationAnalyzer;
import fiji.plugin.trackmate.tracking.LAPUtils;
import fiji.plugin.trackmate.tracking.TrackerKeys;
import fiji.plugin.trackmate.tracking.sparselap.SparseLAPTrackerFactory;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.ImageProcessor;

import java.util.ArrayList;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, headless = true, menuPath = "FMI>Track Spots (Subpixel localization, multi-channel)")
public class TrackMateWrapperMultiChannel implements Command {
	@Parameter
	private LogService log;

	@Parameter(label = "Input image")
	private ImagePlus imp;
	// TODO replace by ImgPlus to keep calibration
	// see https://github.com/knime-ip/knip-imagej2/issues/13

	@Parameter(label = "Target channel")
	private int targetChannel;

	@Parameter(label = "Frame interval")
	private double frameInterval;

	@Parameter(label = "ROI mask", required = false)
	private ImagePlus mask;

	@Parameter(label = "Spot radius")
	private double spotSize = DetectorKeys.DEFAULT_RADIUS;

	@Parameter(label = "Spot quality threshold")
	private double spotThreshold = DetectorKeys.DEFAULT_THRESHOLD;

	@Parameter(label = "Linking max distance")
	private double linkingMaxDistance = TrackerKeys.DEFAULT_LINKING_MAX_DISTANCE;

	@Parameter(label = "Gap closing max distance")
	private double closingMaxDistance = TrackerKeys.DEFAULT_GAP_CLOSING_MAX_DISTANCE;

	@Parameter(label = "Gap closing max frame gap")
	private int frameGap = TrackerKeys.DEFAULT_GAP_CLOSING_MAX_FRAME_GAP;

	@Parameter(label = "Filter max quality spot per frame", required = false)
	private boolean filterMaxQuality = false;

	@Parameter(type = ItemIO.OUTPUT)
	private int nSpotsFound;

	@Parameter(type = ItemIO.OUTPUT)
	private int nTracksFound;

	@Parameter(type = ItemIO.OUTPUT)
	private double[] spotID;

	@Parameter(type = ItemIO.OUTPUT)
	private double[] spotQuality;

	@Parameter(type = ItemIO.OUTPUT)
	private double[] trackID;

	@Parameter(type = ItemIO.OUTPUT)
	private double[] frame;

	@Parameter(type = ItemIO.OUTPUT)
	private double[] t;

	@Parameter(type = ItemIO.OUTPUT)
	private double[] x;

	@Parameter(type = ItemIO.OUTPUT)
	private double[] y;

	@Parameter(type = ItemIO.OUTPUT)
	private double[] z;

	@Parameter(type = ItemIO.OUTPUT)
	private double[] totalIntensity;

	@Parameter(type = ItemIO.OUTPUT)
	private double[] meanIntensity;

	@Parameter(type = ItemIO.OUTPUT)
	private double[] radius;

	@Parameter(type = ItemIO.OUTPUT)
	private double[] estDiameter;

	@Parameter(type = ItemIO.OUTPUT)
	private double[] contrast;

	@Parameter(type = ItemIO.OUTPUT)
	private double[] ch1Intensity;

	@Parameter(type = ItemIO.OUTPUT)
	private double[] ch2Intensity;

	@Parameter(type = ItemIO.OUTPUT)
	private double[] ch3Intensity;

	@Override
	public void run() {
		// Set mask ROI on input image
		if (mask != null) {
			mask.getProcessor().setThreshold(1.0, Double.POSITIVE_INFINITY,
					ImageProcessor.NO_LUT_UPDATE);
			Roi roi = ThresholdToSelection.run(mask);
			imp.setRoi(roi);
		}

		// Create TrackMate instance with settings
		Model model = new Model();
		Settings settings = new Settings();

		settings.setFrom(imp);
		settings.dt = frameInterval;
		settings.detectorFactory = new LogDetectorFactory<>();

		settings.detectorSettings = settings.detectorFactory
				.getDefaultSettings();
		settings.detectorSettings.put(
				DetectorKeys.KEY_DO_SUBPIXEL_LOCALIZATION, true);
		settings.detectorSettings.put(DetectorKeys.KEY_RADIUS, spotSize);
		settings.detectorSettings
				.put(DetectorKeys.KEY_THRESHOLD, spotThreshold);
		settings.detectorSettings.put(DetectorKeys.KEY_TARGET_CHANNEL,
			targetChannel);

		if (filterMaxQuality) {
			settings.addSpotFilter(new FeatureFilter(
					MaxQualitySpotAnalyzerFactory.HAS_MAX_QUALITY_IN_FRAME,
					0.5, true));
		}

		settings.trackerFactory = new SparseLAPTrackerFactory();
		settings.trackerSettings = LAPUtils.getDefaultLAPSettingsMap();
		settings.trackerSettings.put(TrackerKeys.KEY_LINKING_MAX_DISTANCE,
				linkingMaxDistance);
		settings.trackerSettings.put(TrackerKeys.KEY_GAP_CLOSING_MAX_DISTANCE,
				closingMaxDistance);
		settings.trackerSettings.put(TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP,
				frameGap);
		settings.addSpotAnalyzerFactory(new SpotIntensityAnalyzerFactory<>());
		settings.addSpotAnalyzerFactory(new SpotRadiusEstimatorFactory<>());
		settings.addSpotAnalyzerFactory(new SpotContrastAnalyzerFactory<>());
		settings.addSpotAnalyzerFactory(new SpotMultiChannelIntensityAnalyzerFactory<>());
		settings.addSpotAnalyzerFactory(new MaxQualitySpotAnalyzerFactory<>());
		settings.addTrackAnalyzer(new TrackDurationAnalyzer());

		// TODO detect spots first, filter by mask.contains, then do tracking

		TrackMate trackmate = new TrackMate(model, settings);

		// Process (spot detection and tracking)
		if (!trackmate.checkInput()) {
			log.error("Configuration error: " + trackmate.getErrorMessage());
			return;
		}
		if (!trackmate.process()) {
			log.error("Processing error: " + trackmate.getErrorMessage());
			return;
		}

		// Prepare lists to collect results
		ArrayList<Double> spotIDlist = new ArrayList<>();
		ArrayList<Double> qualityList = new ArrayList<>();
		ArrayList<Double> trackIDlist = new ArrayList<>();
		ArrayList<Double> durationList = new ArrayList<>();
		ArrayList<Double> frameList = new ArrayList<>();
		ArrayList<Double> tList = new ArrayList<>();
		ArrayList<Double> xList = new ArrayList<>();
		ArrayList<Double> yList = new ArrayList<>();
		ArrayList<Double> zList = new ArrayList<>();
		ArrayList<Double> totalIntensityList = new ArrayList<>();
		ArrayList<Double> meanIntensityList = new ArrayList<>();
		ArrayList<Double> radiusList = new ArrayList<>();
		ArrayList<Double> diameterList = new ArrayList<>();
		ArrayList<Double> contrastList = new ArrayList<>();

		ArrayList<Double> ch1List = new ArrayList<>();
		ArrayList<Double> ch2List = new ArrayList<>();
		ArrayList<Double> ch3List = new ArrayList<>();

		TrackModel trackModel = model.getTrackModel();
		FeatureModel featureModel = model.getFeatureModel();
		for (Integer tID : trackModel.trackIDs(false)) {
			for (Spot spot : trackModel.trackSpots(tID)) {
				spotIDlist.add((double) spot.ID());
				qualityList.add(spot.getFeature(Spot.QUALITY));
				trackIDlist.add((double) tID);
				durationList.add(featureModel.getTrackFeature(tID,
						TrackDurationAnalyzer.TRACK_DURATION));
				frameList.add(spot.getFeature(Spot.FRAME));
				tList.add(spot.getFeature(Spot.POSITION_T));
				xList.add(spot.getDoublePosition(0));
				yList.add(spot.getDoublePosition(1));
				zList.add(spot.getDoublePosition(2));
				totalIntensityList
						.add(spot
								.getFeature(SpotIntensityAnalyzerFactory.TOTAL_INTENSITY));
				meanIntensityList
						.add(spot
								.getFeature(SpotIntensityAnalyzerFactory.MEAN_INTENSITY));
				radiusList.add(spot.getFeature(Spot.RADIUS));
				diameterList
						.add(spot
								.getFeature(SpotRadiusEstimatorFactory.ESTIMATED_DIAMETER));
				contrastList.add(spot
						.getFeature(SpotContrastAnalyzerFactory.KEY));
				ch1List.add(spot.getFeature(SpotMultiChannelIntensityAnalyzerFactory.FEATURES.get(0)));
				ch2List.add(spot.getFeature(SpotMultiChannelIntensityAnalyzerFactory.FEATURES.get(1)));
				ch3List.add(spot.getFeature(SpotMultiChannelIntensityAnalyzerFactory.FEATURES.get(2)));
			}
		}

		/*
		 * // Get spot collection (all spots) SpotCollection spotCollection =
		 * model.getSpots(); for (Spot spot : spotCollection.iterable(false)) {
		 * spotIDlist.add((double) spot.ID()); trackIDlist.add((double)
		 * spot.ID()); // TODO trackID xList.add(spot.getDoublePosition(0));
		 * yList.add(spot.getDoublePosition(1));
		 * zList.add(spot.getDoublePosition(2)); }
		 */

		// Get results (spot list with trackIDs)
		spotID = Doubles.toArray(spotIDlist);
		spotQuality = Doubles.toArray(qualityList);
		trackID = Doubles.toArray(trackIDlist);
		frame = Doubles.toArray(frameList);
		t = Doubles.toArray(tList);
		x = Doubles.toArray(xList);
		y = Doubles.toArray(yList);
		z = Doubles.toArray(zList);
		totalIntensity = Doubles.toArray(totalIntensityList);
		meanIntensity = Doubles.toArray(meanIntensityList);
		radius = Doubles.toArray(radiusList);
		estDiameter = Doubles.toArray(diameterList);
		contrast = Doubles.toArray(contrastList);

		ch1Intensity = Doubles.toArray(ch1List);
		ch2Intensity = Doubles.toArray(ch2List);
		ch3Intensity = Doubles.toArray(ch3List);

		// Return summary values
		nSpotsFound = model.getSpots().getNSpots(false);
		nTracksFound = model.getTrackModel().nTracks(false);
	}

}
