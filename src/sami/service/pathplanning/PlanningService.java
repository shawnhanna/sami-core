package sami.service.pathplanning;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import sami.environment.Continuous3DEnvironment;
import sami.path.DestinationObjective;
import sami.path.DestinationUtmObjective;
import sami.path.Location;
import sami.path.PathUtm;
import sami.path.STANAGPlanLite;
import sami.path.STANAGWaypointLite;
import sami.path.UTMCoordinate;
import sami.service.pathplanning.algorithm.AStar;
import sami.service.pathplanning.algorithm.PRM;
import sami.service.pathplanning.testing.Viewer;
import sami.service.pathplanning.vehicle.SimpleVehicle;

/**
 *
 * @author pscerri
 */
public class PlanningService {

    private final static Logger LOGGER = Logger.getLogger(PlanningService.class.getName());
    static private boolean showPlan = true;

    static public void submitRequest(final PlanningServiceRequest request, final PlanningServiceListenerInt listener) {
        if (request.getOf() instanceof DestinationUtmObjective) {
            returnFakeResponse(request, listener);
            return;
        }

        PlanningServiceListenerInt l2 = null;

        if (showPlan) {
            l2 = new PlanningServiceListenerInt() {
                @Override
                public void responseRecieved(PlanningServiceResponse response) {

                    new Viewer(request.getEm(), response.getPath());
                    listener.responseRecieved(response);
                }
            };
        }

        final PlanningServiceListenerInt l = l2 == null ? listener : l2;

        // @todo Options might actually call different planners
        (new Thread() {
            public void run() {

                LOGGER.log(Level.FINE, "Got request for " + request.getNoOptions() + " paths", this);

                if (request.getEm() instanceof Continuous3DEnvironment) {

                    // @todo Choose between RRT and PRM and Potential Fields
                    try {

                        // RRT rrt = new RRT(request.getEm(), (SimpleVehicle) request.getVeh(), (DestinationObjective) request.getOf(), l, request.getNoOptions());
                        int noNodes = 50;
                        double roadLength = 0.2;
                        PRM prm = new PRM(request.getEm(),
                                (SimpleVehicle) request.getVeh(),
                                (DestinationObjective) request.getOf(),
                                l,
                                request.getNoOptions(),
                                noNodes,
                                roadLength);

                    } catch (Exception e) {
                        System.out.println("RRT failed");
                    }

                } else {

                    try {
                        AStar astar = new AStar(request.getEm(), (SimpleVehicle) request.getVeh(), (DestinationObjective) request.getOf(), l, request.getNoOptions());
                    } catch (ClassCastException e) {
                        System.out.println("Wrong types for AStar ... ");
                    } catch (NullPointerException e) {
                        System.out.println("Planner failed: " + e);
                        e.printStackTrace();
                    }
                }

            }
        }).start();
    }

    static private void returnFakeResponse(final PlanningServiceRequest request, final PlanningServiceListenerInt l) {
        (new Thread() {
            public void run() {

                try {
                    sleep(500);
                } catch (InterruptedException ex) {
                    Logger.getLogger(PlanningService.class.getName()).log(Level.SEVERE, "Sleep failed??!", ex);
                }

                if (request.getOf() != null && request.getOf() instanceof DestinationUtmObjective) {
                    try {
                        DestinationUtmObjective objective = (DestinationUtmObjective) request.getOf();
                        ArrayList<Location> wps = new ArrayList<Location>();
                        // Add a slight offset to the current location, otherwise
                        //  the proxy will spin in place indefinitely
                        Location start = objective.getStartLocation();
                        Location startOffset = new Location(new UTMCoordinate(start.getCoordinate().getNorthing() + 1, start.getCoordinate().getEasting(), start.getCoordinate().getZone()), start.getAltitude());
                        //wps.add(startOffset);

                        //Code to receive lat/long waypoints from the SBPL planner
                        String serverAddress = "localhost";
                        Socket socket = new Socket(serverAddress, 9090);
                        if (socket.isConnected()) {
                            System.out.println("Connected to waypoint server");
                        }

                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        BufferedReader input = new BufferedReader(new InputStreamReader(
                                socket.getInputStream()));

                        try {
                            Thread.currentThread().sleep(100);
                        } catch (Exception e) {
                        }

                        if (socket.isConnected()) {
                            String s = "getpath " + startOffset.getCoordinate().getEasting() + " " + startOffset.getCoordinate().getNorthing() + " " + objective.getEndLocation().getCoordinate().getEasting() + " " + objective.getEndLocation().getCoordinate().getNorthing() + "\n";
                            System.out.print("Sending 'get path request': " + s);
                            out.print(s);
                            out.println();
                        }

                        ArrayList<Double> latitude = new ArrayList<Double>();
                        ArrayList<Double> longitude = new ArrayList<Double>();
                        try {
                            while (true) {
                                String response = "";
                                //if (input.ready()) {
                                response = input.readLine();
								// } else {
								// 	System.out.println("Socket input stream not ready");
								// }
                                if (response.equals("points end")) {
                                    break;
                                } else {
                                    String[] array = response.split(" ");
                                    latitude.add(Double.parseDouble(array[0]));
                                    longitude.add(Double.parseDouble(array[1]));
                                    Location newPose = new Location(new UTMCoordinate(latitude.get(latitude.size() - 1), longitude.get(longitude.size() - 1)), start.getAltitude());
                                    wps.add(newPose);
                                    LOGGER.log(Level.INFO, "lat_long: " + latitude.get(latitude.size() - 1) + " " + longitude.get(longitude.size() - 1));
                                }
                            }
                            System.out.println(latitude);
                            System.out.println(longitude);
                        } catch (IOException ex) {
                            LOGGER.log(Level.SEVERE, "IO EXCEPTION");
                            Logger.getLogger(PlanningService.class.getName()).log(Level.SEVERE, null, ex);
                        } finally {
                            LOGGER.log(Level.INFO, "closing connection to socket");
                            try {
                                out.close();
                                input.close();
                                socket.close();
                            } catch (IOException ex) {
                                Logger.getLogger(PlanningService.class.getName()).log(Level.SEVERE, null, ex);
                            }

                        }

                        LOGGER.info("Received " + latitude.size() + " coordinates from the planner");

//                        wps.add(objective.getEndLocation());
                        PathUtm path = new PathUtm(wps);
                        ArrayList<PathUtm> altPaths = new ArrayList<PathUtm>();
                        for (int i = 1; i < request.getNoOptions(); i++) {
                            ArrayList<Location> altWps = new ArrayList<Location>();
                            // Add a slight offset to the current location, otherwise
                            //  the proxy will spin in place indefinitely
                            start = objective.getStartLocation();
                            startOffset = new Location(new UTMCoordinate(start.getCoordinate().getNorthing() + 1, start.getCoordinate().getEasting(), start.getCoordinate().getZone()), start.getAltitude());
                            altWps.add(startOffset);
                            altWps.add(objective.getEndLocation());
                            altPaths.add(new PathUtm(altWps));
                        }
                        PlanningServiceResponse response = new PlanningServiceResponse(path, altPaths);
                        LOGGER.log(Level.INFO, "Responses: " + (response.getAlternatives() == null ? 1 : response.getAlternatives().size() + 1), this);
                        l.responseRecieved(response);
                    } catch (IOException ex) {
                        Logger.getLogger(PlanningService.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    PlanningServiceResponse response = new PlanningServiceResponse(path, altPaths);

                    LOGGER.log(Level.FINE, "Responses: " + (response.getAlternatives() == null ? 1 : response.getAlternatives().size() + 1), this);
                    l.responseRecieved(response);
                }
            }
        }).start();
    }

    static STANAGPlanLite createDummySTANAGPlan() {
        STANAGPlanLite plan = new STANAGPlanLite();
        ArrayList<STANAGWaypointLite> wps = new ArrayList<STANAGWaypointLite>();

        for (int i = 0; i < 3; i++) {
            wps.add(new STANAGWaypointLite());
        }

        plan.setWaypoints(wps);
        return plan;

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
    }
}
