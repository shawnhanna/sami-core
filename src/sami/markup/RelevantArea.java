package sami.markup;

import sami.area.Area2D;

/**
 *
 * @author nbb
 */
public class RelevantArea extends Markup {

    public Area2D area;
    
    // Specify area or proxy or point
    
    public enum AreaSelection { AREA, CENTER_PROXY, CENTER_POINT };
    public enum MapType { SATELLITE, POLITICAL };
    
    public RelevantArea() {
    }
    
    public RelevantArea(Area2D area) {
        this.area = area;
    }
}
