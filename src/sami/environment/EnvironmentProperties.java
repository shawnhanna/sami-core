package sami.environment;

import java.io.Serializable;
import java.util.ArrayList;
import sami.path.Location;

/**
 *
 * @author nbb
 */
public class EnvironmentProperties implements Serializable {

    private ArrayList<ArrayList<Location>> obstacleList = new ArrayList<ArrayList<Location>>();
    
    public ArrayList<ArrayList<Location>> getObstacleList() {
        return obstacleList;
    }
    
    public void setObstacleList(ArrayList<ArrayList<Location>> obstacleList) {
        this.obstacleList = obstacleList;
    }
}
