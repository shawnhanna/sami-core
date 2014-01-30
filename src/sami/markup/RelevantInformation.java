
package sami.markup;

/**
 *
 * @author nbb
 */
public class RelevantInformation extends Markup {
    
    public enum Information { SPECIFY };
    public enum Visualization { HEATMAP, CONTOUR, THRESHOLD };

    public static void main(String[] args) {
        System.out.println(Information.SPECIFY.hashCode());
        System.out.println(Visualization.HEATMAP.hashCode());
        System.out.println(Visualization.CONTOUR.hashCode());
        System.out.println(Visualization.THRESHOLD.hashCode());
        System.out.println(Information.SPECIFY.toString());
        System.out.println(Visualization.HEATMAP.toString());
        System.out.println(Visualization.CONTOUR.toString());
        System.out.println(Visualization.THRESHOLD.toString());
        
    }
}
