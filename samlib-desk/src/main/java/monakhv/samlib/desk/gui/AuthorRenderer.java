package monakhv.samlib.desk.gui;

import monakhv.samlib.db.entity.Author;
import monakhv.samlib.log.Log;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/*
 * Copyright 2015  Dmitry Monakhov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 2/16/15.
 */
public class AuthorRenderer extends DefaultListCellRenderer {
    private static final String DEBUG_TAG = "AuthorRenderer";

    private final AuthorRow pan ;


    public AuthorRenderer() {
        pan = new AuthorRow();
    }

    @Override
    public Component getListCellRendererComponent(JList list,
                                                  Object value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean hasFocus) {

        if (value != null) {
            if (value instanceof Author) {
                final Author a = (Author) value;
                pan.load(a);
            }

        }

        pan.setSelected(isSelected,list);

        pan.setEnabled(list.isEnabled());
        return pan;


    }

    @Override
    public void revalidate() {
        super.revalidate();
        if (pan==null){
            return;
        }
        pan.revalidate();
    }

    @Override
    public void repaint() {
        super.repaint();
        if (pan == null){
            return;
        }
        pan.repaint();
    }
}
