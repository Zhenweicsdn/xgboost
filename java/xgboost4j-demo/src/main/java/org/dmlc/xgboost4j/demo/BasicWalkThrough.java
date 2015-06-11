/*
 Copyright (c) 2014 by Contributors 

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
    
 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package org.dmlc.xgboost4j.demo;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import org.dmlc.xgboost4j.Booster;
import org.dmlc.xgboost4j.DMatrix;
import org.dmlc.xgboost4j.demo.util.DataLoader;
import org.dmlc.xgboost4j.util.Params;
import org.dmlc.xgboost4j.util.Trainer;
import org.dmlc.xgboost4j.util.WatchList;

/**
 * a simple example of java wrapper for xgboost
 * @author hzx
 */
public class BasicWalkThrough {
    public static boolean checkPredicts(float[][] fPredicts, float[][] sPredicts) {
        if(fPredicts.length != sPredicts.length) {
            return false;
        }
        
        for(int i=0; i<fPredicts.length; i++) {
            if(!Arrays.equals(fPredicts[i], sPredicts[i])) {
                return false;
            }
        }
        
        return true;
    }
    
    
    public static void main(String[] args) throws UnsupportedEncodingException, IOException {
        // load file from text file, also binary buffer generated by xgboost4j
        DMatrix trainMat = new DMatrix("../../demo/data/agaricus.txt.train");
        DMatrix testMat = new DMatrix("../../demo/data/agaricus.txt.test");
        
        //specify parameters
        Params param = new Params() {
            {
                put("eta", 1.0);
                put("max_depth", 2);
                put("silent", 1);
                put("objective", "binary:logistic");
            }
        };
        
        //specify watchList
        WatchList watchs = new WatchList();
        watchs.put("train", trainMat);
        watchs.put("test", testMat);
        
        //set round
        int round = 2;
        
        //train a boost model
        Booster booster = Trainer.train(param, trainMat, round, watchs, null, null);
        
         //predict
        float[][] predicts = booster.predict(testMat);
        
        //save model to modelPath
        File file = new File("./model");
        if(!file.exists()) {
            file.mkdirs();
        }
        
        String modelPath = "./model/xgb.model";
        booster.saveModel(modelPath);
        
        //dump model
        booster.dumpModel("./model/dump.raw.txt", false);
        
        //dump model with feature map
        booster.dumpModel("./model/dump.nice.txt", "../../demo/data/featmap.txt", false);
        
        //save dmatrix into binary buffer
        testMat.saveBinary("./model/dtest.buffer");
        
        //reload model and data
        Booster booster2 = new Booster(param, "./model/xgb.model");
        DMatrix testMat2 = new DMatrix("./model/dtest.buffer");
        float[][] predicts2 = booster2.predict(testMat2);
        
        
        //check the two predicts
        System.out.println(checkPredicts(predicts, predicts2));
        
        System.out.println("start build dmatrix from csr sparse data ...");
        //build dmatrix from CSR Sparse Matrix
        DataLoader.CSRSparseData spData = DataLoader.loadSVMFile("../../demo/data/agaricus.txt.train");
        
        DMatrix trainMat2 = new DMatrix(spData.rowHeaders, spData.colIndex, spData.data, DMatrix.SparseType.CSR);
        trainMat2.setLabel(spData.labels);
        
        //specify watchList
        WatchList watchs2 = new WatchList();
        watchs2.put("train", trainMat2);
        watchs2.put("test", testMat);
        Booster booster3 = Trainer.train(param, trainMat2, round, watchs2, null, null);
        float[][] predicts3 = booster3.predict(testMat2);
        
        //check predicts
        System.out.println(checkPredicts(predicts, predicts3));
    }
}
