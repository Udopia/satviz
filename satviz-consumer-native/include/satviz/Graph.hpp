#ifndef SATVIZ_GRAPH_HPP_
#define SATVIZ_GRAPH_HPP_

#include <vector>
#include <tuple>
#include <iostream>

#include <ogdf/basic/Graph.h>
#include <ogdf/basic/GraphAttributes.h>

namespace satviz {
namespace graph {

class GraphObserver;

struct NodeInfo {
  int index;
  int heat;
  float x;
  float y;
};

struct EdgeInfo {
  int index1;
  int index2;
  float weight;
};

struct WeightUpdate {
  std::vector<std::tuple<int, int, float> > values;
};

struct HeatUpdate {
  std::vector<std::tuple<int, int> > values;
};

/**
 *
 */
class Graph {
private:
  ogdf::Graph graph;
  ogdf::GraphAttributes attrs;
  std::vector<ogdf::node> node_handles;
  std::vector<GraphObserver*> observers;

  void initAttrs();
  void initNodeHandles();

public:
  Graph(size_t num_nodes);
  /**
   * This variant of the constructor only exists to aid
   * debugging & testing. It should not be used directly.
   */
  Graph(ogdf::Graph &graphToCopy);
  ~Graph() = default;

  ogdf::Graph &getOgdfGraph() { return graph; }
  ogdf::GraphAttributes &getOgdfAttrs() { return attrs; }

  void addObserver(GraphObserver *o);
  void removeObserver(GraphObserver *o);

  void submitWeightUpdate(WeightUpdate &update);
  void submitHeatUpdate(HeatUpdate &update);

  void recalculateLayout();
  void adaptLayout();

  void serialize(std::ostream &stream);
  void deserialize(std::istream &stream);

  NodeInfo queryNode(int index);
  EdgeInfo queryEdge(int index1, int index2);

  double getX(ogdf::node v) { return attrs.x(v); }
  double getY(ogdf::node v) { return attrs.y(v); }
};

} // namespace graph
} // namespace satviz

#endif
