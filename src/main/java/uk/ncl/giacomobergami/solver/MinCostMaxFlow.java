package uk.ncl.giacomobergami.solver;
// Java Program to implement
// the above approach
/// https://www.geeksforgeeks.org/minimum-cost-maximum-flow-from-a-graph-using-bellman-ford-algorithm/
import java.util.*;

public class MinCostMaxFlow {

	// Stores the found edges
	boolean found[];

	// Stores the number of nodes
	int N;

	// Stores the capacity
	// of each edge
	int cap[][];

	int flow[][];

	// Stores the cost per
	// unit flow of each edge
	int cost[][];

	// Stores the distance from each node
	// and picked edges for each node
	int dad[], dist[], pi[];

	static final int INF
		= Integer.MAX_VALUE / 2 - 1;

	// Function to check if it is possible to
	// have a flow from the src to sink
	boolean search(int src, int sink)
	{

		// Initialise found[] to false
		Arrays.fill(found, false);

		// Initialise the dist[] to INF
		Arrays.fill(dist, INF);

		// Distance from the source node
		dist[src] = 0;

		// Iterate until src reaches N
		while (src != N) {

			int best = N;
			found[src] = true;

			for (int k = 0; k < N; k++) {

				// If already found
				if (found[k])
					continue;

				// Evaluate while flow
				// is still in supply
				if (flow[k][src] != 0) {

					// Obtain the total value
					int val
						= dist[src] + pi[src]
						- pi[k] - cost[k][src];

					// If dist[k] is > minimum value
					if (dist[k] > val) {

						// Update
						dist[k] = val;
						dad[k] = src;
					}
				}

				if (flow[src][k] < cap[src][k]) {

					int val = dist[src] + pi[src]
							- pi[k] + cost[src][k];

					// If dist[k] is > minimum value
					if (dist[k] > val) {

						// Update
						dist[k] = val;
						dad[k] = src;
					}
				}

				if (dist[k] < dist[best])
					best = k;
			}

			// Update src to best for
			// next iteration
			src = best;
		}

		for (int k = 0; k < N; k++)
			pi[k]
				= Math.min(pi[k] + dist[k],
						INF);

		// Return the value obtained at sink
		return found[sink];
	}

	HashMap<ConcretePair<Integer, Integer>, List<Integer>> map = null;
	HashSet<Integer> vis  = null;
	int[] d = null;
	boolean[] b = null;
	int[] T = null;

	public void bellman_ford_moore(int r)
	{
		// Saving time for continuous memory allocation
		if (map == null) map = new HashMap<>();
		if (vis == null) vis = new HashSet<>();
		if (T == null) T = new int[N];
		if (b == null) b = new boolean[N];
		if (d == null) d = new int[N];
		if (!vis.add(r)) return; // Not performing any computation if we arlready performed the
		// shortest path on this at some point
		
		for (int u = 0; u<N; u++) {
			if (u != r) {
				T[u] = -1;
				d[u] = Integer.MAX_VALUE;
				b[u] = false;
			} else {
				T[r] = -1;
				d[r] = 0;
				b[r] = true;
			}
		}
		
		Queue<Integer> S = new LinkedList<>();
		S.add(r);
		while (!S.isEmpty()) {
			var u = S.poll();
			b[u] = false;
			for (int v = 0; v<N; v++) {
				if (cost[u][v] > 0) {
					if (d[u] + cost[u][v] < d[v]) {
						if (!b[v]) {
							S.add(v);
							b[v] = true;
						}
						T[v] = u;
						d[v] = d[u] + cost[u][v];
					}
				}
			}
		}

		for (int target = 0; target < N; target++) {
			var actualTarget = target;
			var visitingTarget = target;
			List<Integer> inversePath = new ArrayList<>(N);
			while (T[visitingTarget] != -1) {
				inversePath.add(visitingTarget);
				visitingTarget = T[visitingTarget];
			}
			if (!inversePath.isEmpty()) {
				if (visitingTarget != r) {
					throw new RuntimeException("ERROR IN THE ALGORITHM: all the pathrs should lead to the source!");
				}
				inversePath.add(r);
				Collections.reverse(inversePath);
				map.put(new ConcretePair<>(r, actualTarget), inversePath);
			}
		}
	}

	class Result {
		public double total_flow;
		public double total_cost;
		public Set<List<Integer>> minedPaths;

		public Result(double total_flow, double total_cost) {
			this.total_flow = total_flow;
			this.total_cost = total_cost;
			this.minedPaths = new HashSet<>();
		}

		@Override
		public String toString() {
			return "Result{" +
					"total_flow=" + total_flow +
					", total_cost=" + total_cost +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Result result = (Result) o;
			return Double.compare(result.total_flow, total_flow) == 0 && Double.compare(result.total_cost, total_cost) == 0;
		}

		@Override
		public int hashCode() {
			return Objects.hash(total_flow, total_cost);
		}
	}

	// Function to obtain the maximum Flow
	Result getMaxFlow(int[][] cap, int[][] cost,
					  int src, int sink) {

		this.cap = cap;
		this.cost = cost;

		N = cap.length;
		found = new boolean[N];
		flow = new int[N][N];
		dist = new int[N + 1];
		dad = new int[N];
		pi = new int[N];

		Result result = new Result(0,0);

		// If a path exist from src to sink
		while (search(src, sink)) {

			// Set the default amount
			int amt = INF;
			List<Integer> path = new ArrayList<>();
			for (int x = sink; x != src; x = dad[x])

				amt = Math.min(amt,
							flow[x][dad[x]] != 0
								? flow[x][dad[x]]
								: cap[dad[x]][x]
										- flow[dad[x]][x]);

			for (int x = sink; x != src; x = dad[x]) {
				if (x != sink) path.add(x);
				if (flow[x][dad[x]] != 0) {
					flow[x][dad[x]] -= amt;
					result.total_cost -= amt * cost[x][dad[x]];
				}
				else {
					flow[dad[x]][x] += amt;
					result.total_cost += amt * cost[dad[x]][x];
				}
			}
			Collections.reverse(path);
			result.minedPaths.add(path);
			result.total_flow += amt;
		}

		// Return pair total cost and sink
		return result;
	}
}
