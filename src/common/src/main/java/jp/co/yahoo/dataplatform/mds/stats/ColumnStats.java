/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jp.co.yahoo.dataplatform.mds.stats;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;

import jp.co.yahoo.dataplatform.mds.spread.column.ColumnType;

public class ColumnStats{

  private final String columnName;
  private final SummaryStats totalStats;
  private final Map<String,ColumnStats> childContainer = new HashMap<String,ColumnStats>();
  private final Map<ColumnType,SummaryStats> summaryContainer = new HashMap<ColumnType,SummaryStats>();

  private SummaryStats integrationStats = new SummaryStats();

  public ColumnStats( final String columnName ){
    this.columnName = columnName;
    totalStats = new SummaryStats();
  }

  public String getColumnName(){
    return columnName;
  }

  public SummaryStats getTotalStats(){
    return totalStats;
  }

  public void addSummaryStats( final ColumnType columnType , final SummaryStats summaryStats ){
    summaryContainer.put( columnType , summaryStats );
    totalStats.merge( summaryStats );
  }

  public void mergeSummaryStats( final ColumnType columnType , final SummaryStats summaryStats ){
    if( summaryContainer.containsKey( columnType ) ){
      summaryContainer.get( columnType ).merge( summaryStats );
    }
    else{
      addSummaryStats( columnType , summaryStats );
    }
    totalStats.merge( summaryStats );
  }

  public void addChild( final String childColumnName , final ColumnStats columnStats ){
    childContainer.put( childColumnName , columnStats );
  }

  public Map<String,ColumnStats> getChildColumnStats(){
    return childContainer;
  }

  public Map<ColumnType,SummaryStats> getSummaryStats(){
    return summaryContainer;
  }

  public void merge( final ColumnStats columnStats ){
    Map<String,ColumnStats> mergeColumnStats = columnStats.getChildColumnStats();
    for( Map.Entry<String,ColumnStats> entry : mergeColumnStats.entrySet() ){
      if( childContainer.containsKey( entry.getKey() ) ){
        childContainer.get( entry.getKey() ).merge( entry.getValue() );
      }
      else{
        childContainer.put( entry.getKey() , entry.getValue() );
      }
    }

    Map<ColumnType,SummaryStats> mergeSummaryStas = columnStats.getSummaryStats();
    for( Map.Entry<ColumnType,SummaryStats> entry : mergeSummaryStas.entrySet() ){
      if( summaryContainer.containsKey( entry.getKey() ) ){
        summaryContainer.get( entry.getKey() ).merge( entry.getValue() );
      }
      else{
        summaryContainer.put( entry.getKey() , entry.getValue() );
      }
      totalStats.merge( entry.getValue() );
    }
  }

  public SummaryStats doIntegration(){
    integrationStats = new SummaryStats();
    integrationStats.merge( totalStats );
    for( Map.Entry<String,ColumnStats> entry : childContainer.entrySet() ){
      integrationStats.merge( entry.getValue().doIntegration() );
    }
    return integrationStats;
  }

  public Map<Object,Object> toJavaObject(){
    Map<Object,Object> result = new LinkedHashMap<Object,Object>();
    result.put( "name" , columnName );
    result.put( "total" , totalStats.toJavaObject() );
    result.put( "integratoin_total" , integrationStats.toJavaObject() );
    List<Object> typeList = new ArrayList<Object>();
    for( Map.Entry<ColumnType,SummaryStats> entry : summaryContainer.entrySet() ){
      Map<Object,Object> typeJavaObject = entry.getValue().toJavaObject();
      typeJavaObject.put( "column_type" , entry.getKey().toString() );
      typeList.add( typeJavaObject );
    }
    result.put( "column_types" , typeList );
    Map<Object,Object> childMap = new LinkedHashMap<Object,Object>();
    for( Map.Entry<String,ColumnStats> entry : childContainer.entrySet() ){
      childMap.put( entry.getKey() , entry.getValue().toJavaObject() );
    }
    result.put( "child" , childMap );
    return result;
  }

  public String toString( final String parentName ){
    StringBuffer buffer = new StringBuffer();
    String columnPath;
    if( parentName == null ){
      columnPath = columnName;
    }
    else{
      columnPath = String.format( "%s/%s" , parentName , columnName );
    }

    buffer.append( String.format( "%s(ALL_COLUMN_TYPE) : %s\n" , columnPath , totalStats.toString() ) );
    for( Map.Entry<ColumnType,SummaryStats> entry : summaryContainer.entrySet() ){
      buffer.append( String.format( "%s(%s) : %s\n" , columnPath , entry.getKey() , entry.getValue().toString() ) );
    }

    for( Map.Entry<String,ColumnStats> entry : childContainer.entrySet() ){
      buffer.append( entry.getValue().toString( columnPath ) );
    }

    return buffer.toString();
  }

  @Override
  public String toString(){
    return toString( null );
  }

}
