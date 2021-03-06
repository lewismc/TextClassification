/**
 * Copyright 2009 DigitalPebble Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.digitalpebble.classification.util.scorers;

import java.util.Iterator;
import java.util.Map;

import com.digitalpebble.classification.Document;
import com.digitalpebble.classification.Lexicon;
import com.digitalpebble.classification.Parameters;
import com.digitalpebble.classification.TrainingCorpus;
import com.digitalpebble.classification.Vector;

public class chiSquareAttributeScorer {
	
	 public static AttributeScorer getScorer(TrainingCorpus corpus, Lexicon lexicon){
		  
		  int numAttributes = lexicon.getAttributesNum();
		  
		  AttributeScorer scorer = new AttributeScorer();
		  
		   // build the matrix: attributes x labels
		
		    double[][] matrix = new double[numAttributes][lexicon.getLabelNum()];
		    double [] totalAttributes = new double[numAttributes];
		    double [] totalClasses = new double[lexicon.getLabelNum()];
		    double total = 0d;
		   
		    int[] attributeIDToRank = new int[lexicon.maxAttributeID()+1];
		    java.util.Arrays.fill(attributeIDToRank, -1);
		    
		    int[] attributeRankToID = new int[numAttributes];
		    java.util.Arrays.fill(attributeRankToID, -1);
		    
		    int latestRank = 0;
		    
		    // fill the matrix
		    Iterator<Document> docIter = corpus.iterator();
		    while(docIter.hasNext()){
		      Document d = docIter.next();	
		      // Vector vector = d.getFeatureVector(lexicon);
		      // get a vector based on the number of occurrences i.e on the raw document
		      Vector vector = d.getFeatureVector(lexicon,Parameters.WeightingMethod.OCCURRENCES);
		      int[] indices = vector.getIndices();
		      double[] values = vector.getValues();
		      int classNum = d.getLabel();
		      
		      for (int i=0;i<indices.length;i++){
		        int index = indices[i];
		        double value = values[i];
		        if (value==0) continue;	        
		        // problem here : the index is not the same as the rank
		        // find the rank of this attribute
		        int rank = attributeIDToRank[index];
		        if (rank==-1){
		        	// not seen this one yet
		        	rank = latestRank;
		        	attributeIDToRank[index]= rank;
		        	attributeRankToID[rank]=index;
		        	latestRank++;
		        }
		        
		        matrix[rank][classNum]+= value;
		        totalAttributes[rank]+= value;
		        totalClasses[classNum]+= value;
		        total+=value;
		      }
		    }
		    
		    Map invertedAttributeIndex = lexicon.getInvertedIndex();
		    
		    // attribute by attribute
		    for (int m=0;m<totalAttributes.length;m++){
		      double score4attribute = 0;
		      // (total for this attribute * total for label value) / bigTotal   
		      StringBuffer buffer = new StringBuffer();
		      StringBuffer buffer2 = new StringBuffer();
		      int idAttr = attributeRankToID[m];
		      buffer.append(invertedAttributeIndex.get(idAttr));
		      buffer.append( "[").append(idAttr).append("]");
		      
		      // number of times feature and categ co-occur
		      double A = 0d;
		      // number of times feature occurs without categ
		      double B = 0d;
		      // i.e total feature - A
		      // number of times categ occurs without feature
		      // i.e total categ - A
		      double C = 0d;
		      // number of times neither feature or categ occurs
		      double D = 0d;
		      // total number of occurrences (documents?)
		      double N = total;
		      
		      for (int l=0;l<totalClasses.length;l++){
		        double observed = matrix[m][l];
		        if (observed==0){
		        	buffer2.append("\t").append(observed);
		        	continue;
		        }
		        A = observed;
		        
		        double scoreClasse = (N * (A*D - C*B)*(A*D - C*B)) / ((A+C)*(B+D)*(B+D));
		        buffer2.append("\t").append(scoreClasse);
		        score4attribute += scoreClasse;
		      }
		      score4attribute = 2*score4attribute;
		      scorer.setScore(idAttr, score4attribute);
		      buffer.append("\t").append(score4attribute);
		      buffer.append(buffer2);
		      System.out.println(buffer.toString());
		    }
		  
		  return scorer;	  
	  }
	   
	  /** 
	   * Returns the value of the nth score once sorted
	   * Used to determine whether or not to keep an attribute
	   * **/
	  public static double getValueForRank(int rank,double[] scores){
	    double[] copy = new double[scores.length];
	    System.arraycopy(scores,0,copy,0,scores.length);
	    java.util.Arrays.sort(copy);
	    rank = scores.length-rank;
	    return copy[rank];
	  }
	  

}