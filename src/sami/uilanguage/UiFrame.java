package sami.uilanguage;

import java.util.ArrayList;
import javax.swing.JFrame;

/**
 *
 * @author nbb
 */
public abstract class UiFrame extends JFrame{

    public UiFrame() {
        super();
    }

    public UiFrame(String title) {
        super(title);
    }
    
    public abstract void setGUISpec(ArrayList<sami.gui.GuiElementSpec> guiElements);
}
