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
package jp.co.yahoo.dataplatform.mds.spread.column;

import java.io.IOException;
import java.io.UncheckedIOException;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import java.util.stream.IntStream;
import java.util.stream.Collectors;

import jp.co.yahoo.dataplatform.mds.constants.PrimitiveByteLength;
import jp.co.yahoo.dataplatform.mds.inmemory.IMemoryAllocator;
import jp.co.yahoo.dataplatform.mds.spread.column.filter.IFilter;
import jp.co.yahoo.dataplatform.mds.spread.column.index.ICellIndex;
import jp.co.yahoo.dataplatform.mds.spread.expression.IExpressionIndex;
import jp.co.yahoo.dataplatform.schema.design.IField;
import jp.co.yahoo.dataplatform.schema.design.UnionField;
import jp.co.yahoo.dataplatform.schema.objects.PrimitiveObject;

public class UnionColumn implements IColumn {

  private final Map<ColumnType,IColumn> columnContainer;
  private String columnName;
  private ICellManager cellManager;
  private IColumn parentsColumn = NullColumn.getInstance();
  private ICell defaultCell = NullCell.getInstance();

  public UnionColumn( final IColumn firstColumn ){
    this.columnName = firstColumn.getColumnName();
    firstColumn.setColumnName( firstColumn.getColumnType().toString() );
    cellManager = new CellManager();
    columnContainer = new HashMap<ColumnType,IColumn>();

    IntStream.range( 0 , firstColumn.size() )
      .forEach( i -> cellManager.add( firstColumn.get(i) , i ) );
    columnContainer.put( firstColumn.getColumnType() , firstColumn );
  }

  public UnionColumn( final String columnName , Map<ColumnType,IColumn> columnContainer ){
    this.columnName = columnName;
    this.columnContainer = columnContainer;
    cellManager = new CellManager();
  }

  public void setColumn( final IColumn column ){
    columnContainer.put( column.getColumnType() , column );
  }

  @Override
  public void setColumnName( final String columnName ){
    this.columnName = columnName;
  }

  @Override
  public String getColumnName(){
    return columnName;
  }

  @Override
  public ColumnType getColumnType(){
    return ColumnType.UNION;
  }

  @Override
  public void setParentsColumn( final IColumn parentsColumn ){
    this.parentsColumn = parentsColumn;
  }

  @Override
  public IColumn getParentsColumn(){
    return parentsColumn;
  }

  @Override
  public int add( final ColumnType type , final Object obj , final int index ) throws IOException{
    if( ! columnContainer.containsKey( type ) ){
      columnContainer.put( type , ColumnFactory.get( type , type.toString() ) );
    }
    IColumn column = columnContainer.get( type );
    int totalBytes = column.add( type , obj , index );
    cellManager.add( column.get(index) , index );
    return totalBytes + PrimitiveByteLength.JAVA_OBJECT_LENGTH;
  }

  @Override
  public void addCell( final ColumnType type , final ICell cell , final int index ) throws IOException{
    cellManager.add( cell , index );
  }

  @Override
  public ICellManager getCellManager(){
    return cellManager;
  }

  @Override
  public void setCellManager( final ICellManager cellManager ){
    this.cellManager = cellManager;
  }

  @Override
  public ICell get( final int index ){
    return cellManager.get( index , defaultCell );
  }

  @Override
  public List<String> getColumnKeys(){
    return new ArrayList<String>();
  }

  @Override
  public int getColumnSize(){
    return 0;
  }

  @Override
  public List<IColumn> getListColumn(){
    List<IColumn> result = new ArrayList<IColumn>();
    for( Map.Entry<ColumnType,IColumn> entry : columnContainer.entrySet() ){
      result.add( entry.getValue() );
    }
    return result;
  }

  @Override
  public IColumn getColumn( final int index ){
    return NullColumn.getInstance();
  }

  @Override
  public IColumn getColumn( final String columnName ){
    return NullColumn.getInstance();
  }

  @Override
  public IColumn getColumn( final ColumnType type ){
    if( columnContainer.containsKey( type ) ){
      return columnContainer.get( type );
    }
    return NullColumn.getInstance();
  }

  @Override
  public void setDefaultCell( final ICell defaultCell ){
    this.defaultCell = defaultCell;
  }

  @Override
  public int size(){
    return cellManager.size();
  }

  @Override
  public IField getSchema() throws IOException {
    return getSchema( getColumnName() );
  }

  @Override
  public void setIndex( final ICellIndex index ){
  }

  @Override
  public boolean[] filter( final IFilter filter , final boolean[] filterArray ) throws IOException{
    return null;
  }

  @Override
  public PrimitiveObject[] getPrimitiveObjectArray(final IExpressionIndex indexList , final int start , final int length ){
    return cellManager.getPrimitiveObjectArray( indexList , start , length );
  }

  @Override
  public void setPrimitiveObjectArray(final IExpressionIndex indexList , final int start , final int length , final IMemoryAllocator allocator ) throws IOException{
    cellManager.getPrimitiveObjectArray( indexList , start , length );
  }

  @Override
  public IField getSchema( final String schemaName ) throws IOException{
    UnionField schema = new UnionField( schemaName );
    columnContainer.entrySet().stream()
      .forEach( entry -> {
        try{
          schema.set( entry.getValue().getSchema( entry.getKey().toString() ) );
        }catch( IOException e ){
          throw new UncheckedIOException( "IOException addRow in lambda." , e );
        }
      } );
    return schema;
  }

  @Override
  public String toString(){
    StringBuffer result = new StringBuffer();
    result.append( String.format( "Column name : %s\n" , getColumnName() ) );
    result.append( String.format( "Column type : %s<" , getColumnType() ) );
    result.append( columnContainer.entrySet().stream()
      .map( entry -> String.format( "%s" , entry.getValue().getColumnType() ) )
      .collect( Collectors.joining( "," ) )
    );
    result.append( ">\n" );
    result.append( "--------------------------\n" );
    IntStream.range( 0 , size() )
      .forEach( i -> {
        result.append( String.format( "CELL-%d: %s\n" , i , get( i ).toString() ) );
      } );

    return result.toString();
  }

}
