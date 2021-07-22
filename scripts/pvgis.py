import os.path
import requests
import sys
import json

base_url = "https://re.jrc.ec.europa.eu/api/seriescalc"
params = {}


def reset_params():
    initial_value = {
        'outputformat': 'json'
    }
    global params
    params = initial_value


def parse_arguments():
    if len(sys.argv) < 2:
        print(
            "Error: please provide input file with datacenters configuration.\n Ex: python pvgis_script.py config.json")
        sys.exit(1)
    return sys.argv[1]


def parse_json_field(input_json, field):
    return input_json[field] if field in input_json else ''


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
    params['angle'] = parse_json_field(source, 'angle')
    # aspect, trackingtype
    print(params, end="\n")
    response = requests.get(base_url, params=params)
    return response.content


def write_pvgis_data_to_file(datacenter_name, energy_source_name, content, date):
    filename = f'{datacenter_name}-{energy_source_name}-{date}.json'
    #dic_path = "inputFiles/pvgis"
    dic_path = "."
    with open(os.path.join(dic_path, filename), 'wb+') as f:
        f.write(content)


def parse_datacenter(datacenter, simulation_year):
    datacenter_name = datacenter['name']
    energy_sources = datacenter['energySources']
    for energy_source in energy_sources:
        content = fetch_data_from_pvgis(energy_source, simulation_year)
        write_pvgis_data_to_file(datacenter_name, energy_source['name'], content, simulation_year)


def read_config_file(json_file):
    with open(json_file) as file:
        data = json.load(file)

    simulation_year = str(data['simulationDate']).split(":", 1)[0][:-4]
    for datacenter in data['edgeDatacenters']:
        parse_datacenter(datacenter, simulation_year)
    for datacenter in data['cloudDatacenters']:
        parse_datacenter(datacenter, simulation_year)


if __name__ == '__main__':
    input_file = parse_arguments()
    read_config_file(input_file)
