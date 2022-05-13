import json
import os
import SumoNetVis
import matplotlib.pyplot as plt
from SumoNetVis import USA_STYLE
import seaborn as sns
import imageio
import numpy as np
import progressbar
import yaml
from pathlib import Path
import xml.etree.ElementTree as et
import sys
import csv

widgets=[
    ' [', progressbar.Timer(), '] ',
    progressbar.Bar(),
    ' (', progressbar.ETA(), ') ',
]



def rgb_to_255(t):
    return tuple([int(t[0] * 255), int(t[1]*255), int(t[2]*255)])

def generate_palette(N):
    return map(lambda x: '#%02x%02x%02x' % rgb_to_255(x), sns.color_palette(n_colors=N))

class Point(object):
    def __init__(self, x = 0, y = 0, col = '#%02x%02x%02x' % rgb_to_255(tuple([0,0,0]))):
        self.x = x
        self.y = y
        self.col = col

class SumoToOsmosisGUI(object):
    def __init__(self, network, trajectory, intersection_file, sem_pos):
        self.semaphores = dict()
        self.network = network
        self.trajectories = SumoNetVis.Trajectories()
        self.trajectories.read_from_fcd(trajectory)

        trajScanMap = dict()
        for veh in self.trajectories:
            trajScanMap[veh.id] = dict()
            idx = 0
            for time in veh.time:
                trajScanMap[veh.id][time] = idx
                idx = idx + 1

        self.semaphores[""] = Point()
        intersections = dict()
        with open(intersection_file) as f:
            intersections = json.load(f)
            for time in intersections:
                for sem in intersections[time]:
                    intersections[time][sem] = set(intersections[time][sem])
                    self.semaphores[sem] = Point(0,0)

        self.semx = list()
        self.semy = list()
        self.semcol = list()
        with open(sem_pos, mode='r') as csv_file_sem:
            csv_reader = list(csv.DictReader(csv_file_sem))
            for (row, col) in zip(csv_reader, generate_palette(len(csv_reader))):
                id = row["Id"]
                self.semaphores[id].x = float(row["X"])
                self.semaphores[id].y = float(row["Y"])
                self.semaphores[id].col = col
                self.semx.append(self.semaphores[id].x)
                self.semy.append(self.semaphores[id].y)
                self.semcol.append(self.semaphores[id].col)

        idx = 0
        trajName = dict()
        for traj in self.trajectories:
            trajName[traj.id] = idx
            traj.point_plot_kwargs["ms"] = 10
            idx = idx + 1

        for time in intersections:
            for sem in intersections[time]:
                for veh in intersections[time][sem]:
                    self.trajectories.trajectories[trajName[veh]].colors[trajScanMap[veh][float(time)]] = self.semaphores[sem].col

    def generate(self, target):
        net = SumoNetVis.Net(self.network)
        fig, ax = plt.subplots()
        fig.set_figheight(10)
        fig.set_figwidth(16)

        filenames = []
        net.plot(ax, style=USA_STYLE, stripe_width_scale=3)
        bar = progressbar.ProgressBar(maxval=int(self.trajectories.end / self.trajectories.timestep), widgets=widgets).start()
        idx = 1
        for time in np.arange(self.trajectories.start, self.trajectories.end+self.trajectories.timestep, self.trajectories.timestep):
            self.trajectories.plot_points(time, ax, animate_color=True)
            f = str(time) + ".png"
            filenames.append(f)
            ax.scatter(self.semx, self.semy, color=self.semcol)
            plt.savefig(f)
            bar.update(idx)
            idx = idx + 1

        images = []
        for filename in filenames:
            images.append(imageio.v2.imread(filename))
        imageio.mimsave(target, images)
        for filename in filenames:
            os.remove(filename)



# Press the green button in the gutter to run the script.
if __name__ == '__main__':
    if len(sys.argv)>1:
        with open(sys.argv[1], "r") as stream:
            try:
                d = yaml.safe_load(stream)
                cfg = os.path.abspath(d["sumo_configuration_file_path"])
                root = et.parse(cfg)
                text = root.getroot().find("input/net-file").attrib["value"]
                netFile = str(Path(cfg).parent / text)
                sumo_trace = str(os.path.abspath(d["trace_file"]))
                tracesMatch = str(Path(os.path.abspath(d["OsmosisOutput"])) / (d["experimentName"]+"_tracesMatch.json"))
                sem_pos = str(Path(os.path.abspath(d["OsmosisOutput"])) / (d["experimentName"]+"_tls.csv"))
                print(str(tracesMatch))
                SumoToOsmosisGUI(netFile,
                                 sumo_trace,
                                 tracesMatch,
                                 sem_pos).generate(os.path.abspath(d["SimulationOutGif"]))
            except yaml.YAMLError as exc:
                print(exc)
    else:
        print("ERROR: you should provide the YAML configuration file in Java, so to generate the simulation output!")


# See PyCharm help at https://www.jetbrains.com/help/pycharm/
