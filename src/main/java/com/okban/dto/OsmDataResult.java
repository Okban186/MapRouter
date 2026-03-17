package com.okban.dto;

import java.util.List;

import com.okban.model.GraphStorage;
import com.okban.uiLayer.Abstract.MapFeature;

public class OsmDataResult {
    public GraphStorage graphStorage;
    public List<MapFeature>[][] tileIndexs;
    public long[] tileSegmentIds;

    public OsmDataResult(GraphStorage graphStorage, List<MapFeature>[][] tileIndexs, long[] tileSegmentIds) {
        this.graphStorage = graphStorage;
        this.tileIndexs = tileIndexs;
        this.tileSegmentIds = tileSegmentIds;
    }
}
