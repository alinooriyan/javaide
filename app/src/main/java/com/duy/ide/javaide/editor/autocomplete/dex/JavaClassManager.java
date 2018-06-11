/*
 * Copyright (C) 2018 Tran Le Duy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.duy.ide.javaide.editor.autocomplete.dex;

import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.EditText;

import com.duy.ide.javaide.editor.autocomplete.internal.PatternFactory;
import com.duy.ide.javaide.editor.autocomplete.util.JavaUtil;

import java.util.ArrayList;
import java.util.regex.Pattern;

import static com.duy.ide.javaide.editor.autocomplete.internal.PatternFactory.lastMatchStr;
import static com.duy.ide.javaide.editor.autocomplete.util.EditorUtil.getCurrentClassSimpleName;
import static com.duy.ide.javaide.editor.autocomplete.util.EditorUtil.getPossibleClassName;
import static com.duy.ide.javaide.editor.autocomplete.util.EditorUtil.getWord;

/**
 * Created by Duy on 21-Jul-17.
 */

public class JavaClassManager {
    private static final String TAG = "JavaClassManager";

    public static ArrayList<String> determineClassName(EditText editor, int pos, String text,
                                                       @NonNull String prefix) {

        try {
            ArrayList<String> classNames = null;
            String className;
            boolean isInstance;
            isInstance = prefix.matches("\\)$");

            if (prefix.isEmpty() || prefix.equals("this")) {
                className = getCurrentClassSimpleName(editor.getText().toString());
            } else {
                String word = getWord(editor, pos);
                if (word.contains("((")) {
                    className = Pattern.compile("[^)]*").matcher(prefix).group(); // TODO: 20-Jul-17  exception
                } else {
                    className = prefix;
                }
            }
            Log.d(TAG, "determineClassName className = " + className);
            if (JavaUtil.isValidClassName(className) && text.contains(".")) {
                int start = Math.max(0, pos - 2500);
                CharSequence range = editor.getText().subSequence(start, pos);

                //BigInteger num = new BigInteger(); -> BigInteger num =
                String instance = lastMatchStr(range, PatternFactory.makeInstance(prefix));
                Log.d(TAG, "determineClassName lastMatchStr className = " + className);
                if (instance != null) {
                    //BigInteger num =  -> BigInteger
                    instance = instance.replaceAll("(\\s?)(" + prefix + ")(\\s?[,;=)])", "").trim(); //clear name
                    //generic ArrayList<String> -> ArrayList
                    className = instance.replaceAll("<.*>", ""); //clear generic
                }
            }

            classNames = new ArrayList<>();
            classNames.addAll(getPossibleClassName(editor.getText().toString(), className, prefix));

            return classNames;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}