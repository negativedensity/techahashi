package trikita.slide;

import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.util.Pair;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import trikita.jedux.Action;
import trikita.jedux.Store;

@Value.Immutable
@Gson.TypeAdapters
public abstract class State {

    //@Nullable
    //public abstract String uri();

    public abstract int page();
    public abstract int cursor();

    public abstract boolean presentationMode();
    public abstract boolean toolbarShown();

    public abstract int colorScheme();

    public abstract boolean plantUMLEnabled();
    public abstract String plantUMLEndPoint();

    public abstract String plantUMLTemplateBefore();
    public abstract String plantUMLTemplateAfter();

    public abstract String templateBefore();
    public abstract String templateAfter();

    public abstract int pdfResolution();

    public abstract int currentPresentation();
    public abstract List<Presentation> presentations();

    @Value.Lazy
    public List<Slide> slides() {
        return Slide.parse(text());
    }

    public String text() {
        return presentations().get(currentPresentation()).text();
    }

    public State withText(String s) {
        ImmutableState.Builder b = ImmutableState.builder()
            .from(this)
            .presentations(Collections.emptyList());

        for(int i = 0; i < presentations().size(); ++i) {
            if (i == currentPresentation())
                b.addPresentations(ImmutablePresentation.copyOf(presentations().get(i)).withText(s));
            else
                b.addPresentations(presentations().get(i));
        }

        return b.build();
    }

    static class Reducer implements Store.Reducer<Action<ActionType, ?>, State> {
        public State reduce(Action<ActionType, ?> a, State s) {
            switch (a.type) {
//                case LOAD_DOCUMENT:
//                    return ImmutableState.copyOf(s)
//                        .withUri((a.value).toString());
                case SET_TEXT:
                    return ImmutableState.copyOf(s).withText((String)a.value);
                case SET_CURSOR:
                    String text = s.text().substring(0, (Integer) a.value);
                    return ImmutableState.copyOf(s)
                        .withPage(Slide.parse(text).size()-1)
                        .withCursor((Integer) a.value);
                case NEXT_PAGE:
                    return ImmutableState.copyOf(s)
                        .withPage(Math.min(s.page()+1, s.slides().size()-1));
                case PREV_PAGE:
                    return ImmutableState.copyOf(s)
                        .withPage(Math.max(s.page()-1, 0));
                case OPEN_PRESENTATION:
                    return ImmutableState.copyOf(s).withPresentationMode(true);
                case CLOSE_PRESENTATION:
                    return ImmutableState.copyOf(s).withPresentationMode(false);
                case TOGGLE_TOOLBAR:
                    return ImmutableState.copyOf(s).withToolbarShown(!s.toolbarShown());
                case SET_COLOR_SCHEME:
                    return ImmutableState.copyOf(s).withColorScheme((Integer) a.value);
                case CONFIGURE_PLANTUML:
                    Pair<Boolean,Pair<String,Pair<String,String>>> configuration = (Pair<Boolean,Pair<String,Pair<String,String>>>)a.value;
                    boolean enabled = configuration.first;
                    String endPoint = configuration.second.first;
                    String templateBefore = configuration.second.second.first;
                    String templateAfter = configuration.second.second.second;
                    return ImmutableState.copyOf(s)
                        .withPlantUMLEnabled(enabled)
                        .withPlantUMLEndPoint(endPoint)
                        .withPlantUMLTemplateBefore(templateBefore)
                        .withPlantUMLTemplateAfter(templateAfter);
                case SET_TEMPLATE:
                    Pair<String,String> beforeAfter = (Pair<String,String>)a.value;
                    return ImmutableState.copyOf(s)
                        .withTemplateBefore(beforeAfter.first)
                        .withTemplateAfter(beforeAfter.second);
                case SET_PDF_RESOLUTION:
                    return ImmutableState.copyOf(s)
                        .withPdfResolution((Integer)a.value);
                case PREVIOUS_PRESENTATION:
                    if(s.currentPresentation() == s.presentations().size()-1 && s.currentPresentation() > 0
                       && (s.text().trim().isEmpty() || s.text().equals(s.presentations().get(s.currentPresentation()-1).text()))) {
                        ((Vibrator) ((Context)a.value).getSystemService(Context.VIBRATOR_SERVICE))
                                .vibrate(50);
                        return ImmutableState.copyOf(s)
                                .withPresentations(s.presentations().subList(0, s.currentPresentation()))
                                .withCurrentPresentation(s.currentPresentation()-1);
                    }
                    else
                        return ImmutableState.copyOf(s)
                            .withCurrentPresentation(
                                Math.max(0, Math.min(s.currentPresentation()-1,s.presentations().size()-1)));
                case NEXT_PRESENTATION:
                    if(!s.text().trim().isEmpty() && s.currentPresentation() == s.presentations().size()-1
                        && (s.currentPresentation() == 0 || !s.text().equals(s.presentations().get(s.currentPresentation()-1).text()))) {
                        ((Vibrator) ((Context)a.value).getSystemService(Context.VIBRATOR_SERVICE))
                                .vibrate(50);
                        return ImmutableState.builder().from(s)
                                .addPresentations(Presentation.Default.build((Context) a.value))
                                .currentPresentation(s.currentPresentation()+1)
                                .build();
                    }
                    else
                        return ImmutableState.copyOf(s)
                            .withCurrentPresentation(
                                Math.max(0, Math.min(s.currentPresentation()+1,s.presentations().size()-1)));
            }
            return s;
        }
    }

    static class Default {
        public static State build(Context c) {
            return ImmutableState.builder()
                    .page(0)
                    .cursor(0)
                    .colorScheme(0)
                    .presentationMode(false)
                    .toolbarShown(true)
                    .plantUMLEnabled(false)
                    .plantUMLEndPoint("https://plantuml.nitorio.us/png")
                    .plantUMLTemplateBefore("skinparam backgroundcolor transparent\nskinparam dpi 300\n")
                    .plantUMLTemplateAfter("")
                    .templateBefore("")
                    .templateAfter("")
                    .pdfResolution(1)
                    .currentPresentation(0)
                    .addPresentations(Presentation.Default.build(c))
                    .build();
        }
    }
}
