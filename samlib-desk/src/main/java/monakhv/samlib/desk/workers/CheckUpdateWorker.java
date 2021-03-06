package monakhv.samlib.desk.workers;

import monakhv.samlib.db.entity.Author;
import monakhv.samlib.service.BookDownloadService;

import javax.swing.*;
import java.util.List;

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
 * 16.07.15.
 */
public class CheckUpdateWorker extends SwingWorker<Void,Void> {
    private BookDownloadService service;
    private List<Author> authors;

    public CheckUpdateWorker(BookDownloadService service, List<Author> authors) {
        this.service = service;
        this.authors = authors;
    }

    @Override
    protected Void doInBackground() throws Exception {
        service.runUpdate(authors);
        return null;
    }
}
