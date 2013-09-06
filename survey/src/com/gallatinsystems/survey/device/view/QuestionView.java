/*
 *  Copyright (C) 2010-2012 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package com.gallatinsystems.survey.device.view;

import java.util.ArrayList;
import java.util.StringTokenizer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import com.gallatinsystems.survey.device.R;
import com.gallatinsystems.survey.device.app.FlowApp;
import com.gallatinsystems.survey.device.domain.AltText;
import com.gallatinsystems.survey.device.domain.Dependency;
import com.gallatinsystems.survey.device.domain.Question;
import com.gallatinsystems.survey.device.domain.QuestionHelp;
import com.gallatinsystems.survey.device.domain.QuestionResponse;
import com.gallatinsystems.survey.device.event.QuestionInteractionEvent;
import com.gallatinsystems.survey.device.event.QuestionInteractionListener;
import com.gallatinsystems.survey.device.util.ConstantUtil;
import com.gallatinsystems.survey.device.util.ViewUtil;

/**
 * basic question view. This just displays the question text. It also provides a
 * mechanism to register/notify QuestionInteractionListeners when a
 * QuestionInteractionEvent occurs. It is unlikely anyone will ever use this
 * class directly (a subclass should be used).
 * 
 * @author Christopher Fagiani
 */
public class QuestionView extends TableLayout implements
        QuestionInteractionListener {
    protected static final int DEFAULT_WIDTH = 290;
    private TextView questionText;

    protected Question question;
    private QuestionResponse response;
    private ArrayList<QuestionInteractionListener> listeners;
    private ImageButton tipImage;
    protected String[] langs = null;
    protected boolean readOnly;
    public static int screenWidth;
    protected String defaultLang;
    
    protected String mLanguage;

    /**
     * install a single tableRow containing a textView with the question text
     * 
     * @param context
     * @param q
     */
    public QuestionView(final Context context, Question q,
            String defaultLangauge, String[] langs, boolean readOnly) {
        super(context);
        question = q;
        this.defaultLang = defaultLangauge;
        this.langs = langs;
        TableRow tr = new TableRow(context);
        questionText = new TextView(context);
        questionText.setWidth(getMaxTextWidth());
        
        // Get custom language
        mLanguage = FlowApp.getApp().getLanguage();

        this.readOnly = readOnly;
        if (!readOnly) {
            questionText.setLongClickable(true);
            questionText.setOnLongClickListener(new OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    ViewUtil.showConfirmDialog(R.string.clearquestion,
                            R.string.clearquestiondesc, context, true,
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    resetQuestion(true);
                                }
                            });
                    return true;
                }
            });
        }
        //questionText.setText(formText(), BufferType.SPANNABLE);
        questionText.setText(getDisplayText(), BufferType.SPANNABLE);
        tr.addView(questionText);

        // if there is a tip for this question, construct an alert dialog box
        // with the data
        final int tips = question.getHelpTypeCount();
        if (tips > 0) {
            tipImage = new ImageButton(context);
            tipImage.setImageResource(android.R.drawable.ic_dialog_info);
            tr.addView(tipImage);
            tipImage.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (tips > 1) {
                        displayHelpChoices();
                    } else {
                        if (question.getHelpByType(ConstantUtil.TIP_HELP_TYPE)
                                .size() > 0) {
                            displayHelp(ConstantUtil.TIP_HELP_TYPE);
                        } else if (question.getHelpByType(
                                ConstantUtil.VIDEO_HELP_TYPE).size() > 0) {
                            displayHelp(ConstantUtil.VIDEO_HELP_TYPE);
                        } else if (question.getHelpByType(
                                ConstantUtil.IMAGE_HELP_TYPE).size() > 0) {
                            displayHelp(ConstantUtil.IMAGE_HELP_TYPE);
                        } else {
                            displayHelp(ConstantUtil.ACTIVITY_HELP_TYPE);
                        }
                    }
                }
            });
        }
        addView(tr);
        // if this question has 1 or more dependencies, then it needs to be
        // invisible initially
        if (question.getDependencies() != null
                && question.getDependencies().size() > 0) {
            setVisibility(View.GONE);
        }
    }
    
    /**
     * Get the (localized) text to display, based on the 
     * user's custom language.
     * If the custom language is not in the AltText map,
     * we will return the default text, either because that's
     * also the custom language, or because we have to use the 
     * default one as a fall-back
     * 
     * @return localized text to be displayed
     */
    private String getDisplayText() {
        StringBuilder builder = new StringBuilder();
        AltText altText = question.getAltText(mLanguage);
        if (altText != null) {
            builder.append(altText.getText());
        } else {
            builder.append(question.getText());
        }
        
        if (question.isMandatory()) {
            builder.append("*");
        }
        
        return builder.toString();
    }

    /**
     * forms the question text based on the selected languages
     * 
     * @return
    private Spanned formText() {
        boolean isFirst = true;
        StringBuilder text = new StringBuilder();
        if (question.isMandatory()) {
            text.append("<i><b>");
        }
        for (int i = 0; i < langs.length; i++) {
            if (defaultLang.equalsIgnoreCase(langs[i])) {
                if (!isFirst) {
                    text.append(" / ");
                } else {
                    isFirst = false;
                }
                text.append(question.getText());
            } else {
                AltText txt = question.getAltText(langs[i]);
                if (txt != null) {
                    if (!isFirst) {
                        text.append(" / ");
                    } else {
                        isFirst = false;
                    }
                    text.append("<font color='").append(colors[i]).append("'>")
                            .append(txt.getText()).append("</font>");
                }
            }
        }
        if (question.isMandatory()) {
            text = text.append("*</b></i>");
        }
        return Html.fromHtml(text.toString());
    }
     */

    /**
     * updates the question's visible languages
     * 
     * @param languageCodes
     */
    public void updateSelectedLanguages(String[] languageCodes) {
        langs = languageCodes;
        //questionText.setText(formText());
    }

    /**
     * displays a dialog box with options for each of the help types that have
     * been initialized for this particular question.
     */
    @SuppressWarnings("rawtypes")
    private void displayHelpChoices() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.helpheading);
        final CharSequence[] items = new CharSequence[question
                .getHelpTypeCount()];
        final Resources resources = getResources();
        int itemIndex = 0;
        ArrayList tempList = question
                .getHelpByType(ConstantUtil.IMAGE_HELP_TYPE);

        if (tempList != null && tempList.size() > 0) {
            items[itemIndex++] = resources.getString(R.string.photohelpoption);
        }
        tempList = question.getHelpByType(ConstantUtil.VIDEO_HELP_TYPE);
        if (tempList != null && tempList.size() > 0) {
            items[itemIndex++] = resources.getString(R.string.videohelpoption);
        }
        tempList = question.getHelpByType(ConstantUtil.TIP_HELP_TYPE);
        if (tempList != null && tempList.size() > 0) {
            items[itemIndex++] = resources.getString(R.string.texthelpoption);
        }
        tempList = question.getHelpByType(ConstantUtil.ACTIVITY_HELP_TYPE);
        if (tempList != null && tempList.size() > 0) {
            items[itemIndex++] = resources
                    .getString(R.string.activityhelpoption);
        }

        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String val = items[id].toString();
                if (resources.getString(R.string.texthelpoption).equals(val)) {
                    displayHelp(ConstantUtil.TIP_HELP_TYPE);
                } else if (resources.getString(R.string.videohelpoption)
                        .equals(val)) {
                    displayHelp(ConstantUtil.VIDEO_HELP_TYPE);
                } else if (resources.getString(R.string.photohelpoption)
                        .equals(val)) {
                    displayHelp(ConstantUtil.IMAGE_HELP_TYPE);
                } else {
                    displayHelp(ConstantUtil.ACTIVITY_HELP_TYPE);
                }
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    /**
     * displays the selected help type
     * 
     * @param type
     */
    private void displayHelp(String type) {
        if (ConstantUtil.VIDEO_HELP_TYPE.equals(type)) {
            notifyQuestionListeners(QuestionInteractionEvent.VIDEO_TIP_VIEW);
        } else if (ConstantUtil.TIP_HELP_TYPE.equals(type)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            TextView tipText = new TextView(getContext());
            StringBuilder textBuilder = new StringBuilder();
            ArrayList<QuestionHelp> helpItems = question.getHelpByType(type);
            if (helpItems != null) {
                for (int i = 0; i < helpItems.size(); i++) {
                    if (i > 0) {
                        textBuilder.append("<br>");
                    }
                    
                    QuestionHelp helpItem = helpItems.get(i);
                    AltText altText = helpItem.getAltText(mLanguage);
                    String text = altText != null ? altText.getText() : helpItem.getText();
                    textBuilder.append(text);
                }
            }
            tipText.setText(Html.fromHtml(textBuilder.toString()));
            builder.setView(tipText);
            builder.setPositiveButton(R.string.okbutton,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            builder.show();
        } else if (ConstantUtil.IMAGE_HELP_TYPE.equals(type)) {
            notifyQuestionListeners(QuestionInteractionEvent.PHOTO_TIP_VIEW);
        } else {
            notifyQuestionListeners(QuestionInteractionEvent.ACTIVITY_TIP_VIEW);
        }
    }

    /**
     * adds a listener to the internal list of clients to be notified on an
     * event
     * 
     * @param listener
     */
    public void addQuestionInteractionListener(
            QuestionInteractionListener listener) {
        if (listeners == null) {
            listeners = new ArrayList<QuestionInteractionListener>();
        }
        if (listener != null && !listeners.contains(listener)
                && listener != this) {
            listeners.add(listener);
        }
    }

    /**
     * notifies each QuestionInteractionListener registered with this question.
     * This is done serially on the calling thread.
     * 
     * @param type
     */
    protected void notifyQuestionListeners(String type) {
        if (listeners != null) {
            QuestionInteractionEvent event = new QuestionInteractionEvent(type,
                    this);
            for (int i = 0; i < listeners.size(); i++) {
                listeners.get(i).onQuestionInteraction(event);
            }
        }
    }

    /**
     * method that can be overridden by sub classes if they want to have some
     * sort of visual response to a question interaction.
     */
    public void questionComplete(Bundle data) {
        // do nothing
    }

    /**
     * method that should be overridden by sub classes to clear current value
     */
    public void resetQuestion(boolean fireEvent) {
        setResponse(null, false);
        highlight(false);
        if (fireEvent) {
            notifyQuestionListeners(QuestionInteractionEvent.QUESTION_CLEAR_EVENT);
        }
    }

    public void onQuestionInteraction(QuestionInteractionEvent event) {
        if (QuestionInteractionEvent.QUESTION_ANSWER_EVENT.equals(event
                .getEventType())) {
            // if this question is dependent, see if it has been satisfied
            ArrayList<Dependency> dependencies = question.getDependencies();
            if (dependencies != null) {
                for (int i = 0; i < dependencies.size(); i++) {
                    Dependency d = dependencies.get(i);
                    if (d.getQuestion().equalsIgnoreCase(
                            event.getSource().getQuestion().getId())) {
                        if (handleDependencyParentResponse(d, event.getSource()
                                .getResponse(true))) {
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * updates the state of this question view based on the value in the
     * dependency parent response. This method returns true if there is a value
     * match and false otherwise.
     * 
     * @param dep
     * @param resp
     * @return
     */
    public boolean handleDependencyParentResponse(Dependency dep,
            QuestionResponse resp) {
        boolean isMatch = false;
        if (dep.getAnswer() != null
                && resp != null
                && dep.isMatch(resp.getValue())
                && (resp.getIncludeFlag() == null || "true"
                        .equalsIgnoreCase(resp.getIncludeFlag()))) {
            isMatch = true;
        } else if (dep.getAnswer() != null
                && resp != null
                && (resp.getIncludeFlag() == null || "true"
                        .equalsIgnoreCase(resp.getIncludeFlag()))) {
            if (resp.getValue() != null) {
                StringTokenizer strTok = new StringTokenizer(resp.getValue(),
                        "|");
                while (strTok.hasMoreTokens()) {
                    if (dep.isMatch(strTok.nextToken().trim())) {
                        isMatch = true;
                    }
                }
            }
        }

        boolean setVisible = false;
        // if we're here, then the question on which we depend
        // has been answered. Check the value to see if it's the
        // one we are looking for
        if (isMatch) {
            setVisibility(View.VISIBLE);
            if (response != null) {
                response.setIncludeFlag("true");
            }
            setVisible = true;
        } else {
            if (response != null) {
                response.setIncludeFlag("false");
            }
            setVisibility(View.GONE);
        }

        // now notify our own listeners to make sure we correctly toggle
        // nested dependencies (i.e. if A -> B -> C and C changes, A needs to
        // know too).
        notifyQuestionListeners(QuestionInteractionEvent.QUESTION_ANSWER_EVENT);

        return setVisible;
    }

    /**
     * this method should be overridden by subclasses so they can record input
     * in a QuestionResponse object
     */
    public void captureResponse() {
        // NO OP
    }

    /**
     * this method should be overridden by subclasses so they can record input
     * in a QuestionResponse object
     */
    public void captureResponse(boolean suppressListeners) {
        // NO OP
    }

    /**
     * this method should be overridden by subclasses so they can manage the UI
     * changes when resetting the value
     * 
     * @param resp
     */
    public void rehydrate(QuestionResponse resp) {
        setResponse(resp, true);
    }

    /**
     * Release any heavy resource associated with this view. This method will
     * likely be overridden by subclasses. This callback should ALWAYS be called
     * when the Activity is about to become invisible (paused, stopped,...) and
     * this View's responses have been successfully cached. Any resource that
     * can cause a memory leak or prevent this View from being GC should be
     * freed/notified
     */
    public void releaseResources() {
    }

    public QuestionResponse getResponse(boolean suppressListeners) {
        if (response == null
                || (ConstantUtil.VALUE_RESPONSE_TYPE.equals(response.getType()) && (response
                        .getValue() == null || response.getValue().trim()
                        .length() == 0))) {
            captureResponse(suppressListeners);
        }
        return response;
    }

    public QuestionResponse getResponse() {
        return getResponse(false);
    }

    public void setResponse(QuestionResponse response) {
        setResponse(response, false);
    }

    public void setResponse(QuestionResponse response, boolean suppressListeners) {
        if (response != null) {
            if (question != null) {
                response.setScoredValue(question.getResponseScore(response
                        .getValue()));
            }
            if (this.response == null) {
                this.response = response;
            } else {
                // we need to preserve the ID so we don't get duplicates in the
                // db
                this.response.setType(response.getType());
                this.response.setValue(response.getValue());
            }
        } else {
            this.response = response;
        }
        if (!suppressListeners) {
            notifyQuestionListeners(QuestionInteractionEvent.QUESTION_ANSWER_EVENT);
        }
    }

    public Question getQuestion() {
        return question;
    }

    public void setTextSize(float size) {
        questionText.setTextSize(size);
    }

    /**
     * hides or shows the tips button
     * 
     * @param isSuppress
     */
    public void suppressHelp(boolean isSuppress) {
        if (isSuppress) {
            if (tipImage != null) {
                tipImage.setVisibility(View.GONE);
            }
        } else {
            if (tipImage != null) {
                tipImage.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * gets the maximum width to use for the first component in the table row
     * (used to ensure the help image icon is on the screen)
     * 
     * @return
     */
    protected int getMaxTextWidth() {
        if (getQuestion().getHelpTypeCount() > 0) {
            return (screenWidth - 90);
        } else {
            return screenWidth;
        }
    }

    public String getDefaultLang() {
        return defaultLang;
    }

    public void setDefaultLang(String defaultLang) {
        this.defaultLang = defaultLang;
    }

    /**
     * turns highlighting on/off
     * 
     * @param useHighlight
     */
    public void highlight(boolean useHighlight) {
        if (useHighlight) {
            questionText.setBackgroundColor(0x55CC99CC);
        } else {
            questionText.setBackgroundColor(Color.TRANSPARENT);
        }
    }
    
}
