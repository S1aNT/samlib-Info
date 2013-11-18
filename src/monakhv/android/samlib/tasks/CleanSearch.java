/*
 * Copyright 2013 Dmitry Monakhov.
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
 */

package monakhv.android.samlib.tasks;

import android.content.Context;
import android.os.AsyncTask;
import monakhv.android.samlib.sql.SearchAuthorController;
import monakhv.android.samlib.sql.entity.AuthorCard;

/**
 *
 * @author Dmitry Monakhov
 */
public class CleanSearch  extends  AsyncTask  <Void, Void, Boolean> {
    private final Context context;
    private final SearchAuthorController sql;
    public CleanSearch(Context ctx){
        context = ctx;
        sql = new SearchAuthorController(context);
    }
    @Override
    protected Boolean doInBackground(Void... params) {
        for (AuthorCard ac : sql.getAll()){
            sql.delete(ac);
        }
        
     return true;
    }
    
}
