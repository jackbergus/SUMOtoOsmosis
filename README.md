# IoTSim-Osmosis-RES

IoTSim-Osmosis-RES is a simulation framework that supports the testing and validation of osmotic computing applications with support of renewable energy sources.

It is based on the IoTSim-Osmosis framework and extended by renewable energy modules and the osmotic agent mechanisms enabling research on renewable energy-aware autonomic IoT systems.

# Main Features

- There are a few primary sources of green energy as solar, wind, water. However, in the framework, the focus is on solar energy with the future possibility of extensions to others energy sources.

- Support for *Osmotic Agents* which are assigned to IoT devices as well as edge and cloud datacenters. Agents implement the autonomic computing *MAPE loop - Monitor, Analyze, Plan and Execute*. Interface-based architecture facilitates the end-user to implement logic for the agents. The Osmotic Agents can constitute the distributed multi agent environment in which they can cooperate by exchanging messages.

# Usage

Due to the high uncertainty of weather conditions – e.g. wind speed and solar radiation level, the efficient analysis of adaptive osmotic computing management algorithms should be based on historical data regarding renewable energy sources.

Historical data are downloaded from the *Photovoltaic Geographical Information System (PVGIS)* portal. It provides data about solar radiation and PV potential based on satellite image analysis for the 2005-2016 years and can give more detailed monthly, daily, and hourly intervals.

 The data from PVGIS is fetched by python script using the web api provided by PVGIS itself. Information telling script what to fetch is found in json file in folder */inputFiles/res/RES_config.json. To fetch new data it is required to prepare a new json file. Json file consists of simulation date and list of edge and cloud data centers. Each data center holds information about the management policy, type of energy storage, power grid and energy sources. In our case energy sources are solar panels.
```json
{
  "simulationDate": "20160504:0000",
  "edgeDatacenters": [
    {
      "name": "Edge_1",
      "type": "edge",
      "energyManagementPolicy": {
        "className": "someEnergyManagementPolicy"
      },
      "energyStorage": [
        {
          "type": "BATTERY",
          "capacity": "100",
          "currentEnergy": "20"
        }
      ],
      "powerGrid": [
        {
          "country": "Poland",
          "priceForEnergy": "0.2"
        }
      ],
      "energySources": [
        {
          "name": "PV_panels_1",
          "type": "PV_PANELS",
          "technology": "CIS",
          "angle": 50,
          "peakPower": 200,
          "loss": 20,
          "location": {
            "latitude": 50.0,
            "longitude": 20.0,
            "elevation": 0.0
          }
        },
        {
          "name": "PV_panels_2",
          "type": "PV_PANELS",
          "angle": "70",
          "peakPower": 100,
          "loss": 15,
          "location": {
            "latitude": 30.0,
            "longitude": 10.5,
            "elevation": 0.0
          }
        }
      ]
    }
```
After preparing such json file we have to run a python script. To do that we have to provide a path to the prepared json file. For example: ```python pvgis_script.py config.json```
Python script opens provided file, checks simulation year and for each data center parses input and fetches data from PVGIS. Function responsible for fetching data creates a request to the PVGIS web api with params read from the delivered json file.
``` python
def fetch_data_from_pvgis(source, year):
    print(source, end="\n")
    reset_params()
    pvcalculation = 0
    params['lat'] = source['location']['latitude']
    params['lon'] = source['location']['longitude']
    params['startyear'] = year
    params['endyear'] = year
    if all(param in source for param in ('loss', 'peakPower')):
        pvcalculation = 1
        params['loss'] = source['loss']
        params['peakpower'] = source['peakPower']
        params['pvtechchoice'] = parse_json_field(source, 'technology')
    params['pvcalculation'] = pvcalculation

    params['optimalangles'] = 1

    print(params, end="\n")
    response = requests.get(base_url, params=params)
    return response.content
```
Example of params given to the request:
```
{'outputformat': 'json', 'lat': 50.0, 'lon': 20.0, 'startyear': '2016', 'endyear': '2016', 'loss': 20, 'peakpower': 200, 'pvtechchoice': 'CIS', 'pvcalculation': 1}
```
After successfully fetching data, script creates a new json file for each data center in the configuration file and saves data there.

# Publications

*Tomasz Szydlo, Amadeusz Szabala, Nazar Kordiumov, Konrad Siuzdak, Lukasz Wolski, Khaled Alwasel, Fawzy Habeeb, Rajiv Ranjan, "IoTSim-Osmosis-RES: Towards autonomic renewableenergy-aware osmotic computing", accepted, https://doi.org/10.1002/spe.3084*
