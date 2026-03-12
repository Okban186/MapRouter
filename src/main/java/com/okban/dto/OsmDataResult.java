package com.okban.dto;

import java.util.List;

import com.okban.model.GraphNode;
import com.okban.model.GraphStorage;
import com.okban.uiLayer.Abstract.MapFeature;

public class OsmDataResult {
    public GraphStorage graphStorage;
    public List<MapFeature>[][] tileIndexs;

    public OsmDataResult(GraphStorage graphStorage, List<MapFeature>[][] tileIndexs) {
        this.graphStorage = graphStorage;
        this.tileIndexs = tileIndexs;
    }
}
