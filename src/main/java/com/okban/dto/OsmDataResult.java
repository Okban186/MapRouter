package com.okban.dto;

import java.util.List;

import com.okban.model.GraphNode;
import com.okban.uiLayer.Abstract.MapFeature;

public class OsmDataResult {
    public GraphNode[] graphNodes;
    public List<MapFeature>[][] tileIndexs;

    public OsmDataResult(GraphNode[] graphNodes, List<MapFeature>[][] tileIndexs) {
        this.graphNodes = graphNodes;
        this.tileIndexs = tileIndexs;
    }
}
