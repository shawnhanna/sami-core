package sami.uilanguage;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import sami.markup.Markup;

/**
 *
 * @author nbb
 */
public class MarkupComponentHelper {

    public Hashtable<Class, MarkupComponent> componentInstances = new Hashtable<Class, MarkupComponent>();
    public Hashtable<Class, MarkupComponentWidget> widgetInstances = new Hashtable<Class, MarkupComponentWidget>();

    public static int getCreationComponentScore(ArrayList<Class> supportedCreationClasses, ArrayList<Enum> supportedMarkups, ArrayList<Class> widgetClasses, Class creationClass, ArrayList<Markup> markups) {
        int score = -1;
        try {
            if (supportedCreationClasses.contains(creationClass)) {
                score = 0;
            } else {
                for (Class widgetClass : widgetClasses) {
                    Object widgetInstance = widgetClass.newInstance();
                    MarkupComponentWidget widget = (MarkupComponentWidget) widgetInstance;
                    score = Math.max(score, widget.getCreationWidgetScore(creationClass, markups));
                }
            }
            if (score >= 0) {
                for (Markup markup : markups) {
                    ArrayList<String> enumFieldNames = (ArrayList<String>) (markup.getClass().getField("enumFieldNames").get(null));
                    for (String enumFieldName : enumFieldNames) {
                        Enum enumValue = (Enum) markup.getClass().getField(enumFieldName).get(markup);
                        if (supportedMarkups.contains(enumValue)) {
                            score++;
                        } else {
                            for (Class widgetClass : widgetClasses) {
                                Object widgetInstance = widgetClass.newInstance();
                                MarkupComponentWidget widget = (MarkupComponentWidget) widgetInstance;
                                ArrayList<Markup> singleMarkup = new ArrayList<Markup>();
                                singleMarkup.add(markup);
                                int widgetScore = widget.getMarkupScore(singleMarkup);
                                if (widgetScore > 0) {
                                    score += widgetScore;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (InstantiationException ex) {
            Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return score;
    }

    public static int getSelectionComponentScore(ArrayList<Class> supportedSelectionClasses, ArrayList<Enum> supportedMarkups, ArrayList<Class> widgetClasses, Class selectionClass, ArrayList<Markup> markups) {

//        System.out.println("### getSelectionComponentScore: \n\tsupportedSelectionClasses:" + supportedSelectionClasses.toString()
//                + "\n\tsupportedMarkups: " + supportedMarkups.toString()
//                + "\n\twidgetClasses: " + widgetClasses.toString()
//                + "\n\tselectionClass: " + selectionClass.toString()
//                + "\n\tmarkups: " + markups.toString());
        int score = -1;
        try {
            if (supportedSelectionClasses.contains(selectionClass)) {
                score = 0;
            } else {
                for (Class widgetClass : widgetClasses) {
                    Object widgetInstance = widgetClass.newInstance();
                    MarkupComponentWidget widget = (MarkupComponentWidget) widgetInstance;
                    score = Math.max(score, widget.getSelectionWidgetScore(selectionClass, markups));
                }
            }
            if (score >= 0) {
                for (Markup markup : markups) {
                    ArrayList<String> enumFieldNames = (ArrayList<String>) (markup.getClass().getField("enumFieldNames").get(null));
                    for (String enumFieldName : enumFieldNames) {
                        Enum enumValue = (Enum) markup.getClass().getField(enumFieldName).get(markup);
                        if (supportedMarkups.contains(enumValue)) {
                            score++;
                        } else {
                            for (Class widgetClass : widgetClasses) {
                                Object widgetInstance = widgetClass.newInstance();
                                MarkupComponentWidget widget = (MarkupComponentWidget) widgetInstance;
                                ArrayList<Markup> singleMarkup = new ArrayList<Markup>();
                                singleMarkup.add(markup);
                                int widgetScore = widget.getMarkupScore(singleMarkup);
                                if (widgetScore > 0) {
                                    score += widgetScore;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (InstantiationException ex) {
            Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
//        System.out.println("###\tscore: " + score);
        return score;
    }

    public static int getMarkupComponentScore(ArrayList<Enum> supportedMarkups, ArrayList<Class> widgetClasses, ArrayList<Markup> markups) {
        int score = 0;
        try {
            for (Markup markup : markups) {
                ArrayList<String> enumFieldNames = (ArrayList<String>) (markup.getClass().getField("enumFieldNames").get(null));
                for (String enumFieldName : enumFieldNames) {
                    Enum enumValue = (Enum) markup.getClass().getField(enumFieldName).get(markup);
                    if (supportedMarkups.contains(enumValue)) {
                        score++;
                    } else {
                        for (Class widgetClass : widgetClasses) {
                            Object widgetInstance = widgetClass.newInstance();
                            MarkupComponentWidget widget = (MarkupComponentWidget) widgetInstance;
                            ArrayList<Markup> singleMarkup = new ArrayList<Markup>();
                            singleMarkup.add(markup);
                            int widgetScore = widget.getMarkupScore(singleMarkup);
                            if (widgetScore > 0) {
                                score += widgetScore;
                                break;
                            }
                        }
                    }
                }
            }
        } catch (InstantiationException ex) {
            Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return score;
    }

    public static int getCreationWidgetScore(ArrayList<Class> supportedCreationClasses, ArrayList<Enum> supportedMarkups, Class creationClass, ArrayList<Markup> markups) {
        int score = -1;
        try {
            if (supportedCreationClasses.contains(creationClass)) {
                score = 0;
                for (Markup markup : markups) {
                    ArrayList<String> enumFieldNames = (ArrayList<String>) (markup.getClass().getField("enumFieldNames").get(null));
                    for (String enumFieldName : enumFieldNames) {
                        Enum enumValue = (Enum) markup.getClass().getField(enumFieldName).get(markup);
                        if (supportedMarkups.contains(enumValue)) {
                            score++;
                            break;
                        }
                    }
                }
            }
        } catch (IllegalAccessException ex) {
            Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return score;
    }

    public static int getSelectionWidgetScore(ArrayList<Class> supportedSelectionClasses, ArrayList<Enum> supportedMarkups, Class selectionClass, ArrayList<Markup> markups) {
        int score = -1;
        try {
            if (supportedSelectionClasses.contains(selectionClass)) {
                score = 0;
                for (Markup markup : markups) {
                    ArrayList<String> enumFieldNames = (ArrayList<String>) (markup.getClass().getField("enumFieldNames").get(null));
                    for (String enumFieldName : enumFieldNames) {
                        Enum enumValue = (Enum) markup.getClass().getField(enumFieldName).get(markup);
                        if (supportedMarkups.contains(enumValue)) {
                            score++;
                            break;
                        }
                    }
                }
            }
        } catch (IllegalAccessException ex) {
            Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return score;
    }

    public static int getMarkupWidgetScore(ArrayList<Enum> supportedMarkups, ArrayList<Markup> markups) {
        int score = 0;
        try {
            for (Markup markup : markups) {
                ArrayList<String> enumFieldNames = (ArrayList<String>) (markup.getClass().getField("enumFieldNames").get(null));
                for (String enumFieldName : enumFieldNames) {
                    Enum enumValue = (Enum) markup.getClass().getField(enumFieldName).get(markup);
                    if (supportedMarkups.contains(enumValue)) {
                        score++;
                        break;
                    }
                }
            }
        } catch (IllegalAccessException ex) {
            Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(MarkupComponentHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return score;
    }
}
