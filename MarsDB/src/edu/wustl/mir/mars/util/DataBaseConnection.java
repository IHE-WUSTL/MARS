/*
 * Copyright 2011 Washington University in St. Louis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.wustl.mir.mars.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

/**
 * Encapsulates information pertaining to a particular DB connection.
 * @author rmoult01
 */
public class DataBaseConnection {

   public Connection conn;
   public ResultSet lastResultSet;
   public ResultSetMetaData lastSQLMetaData;
}
