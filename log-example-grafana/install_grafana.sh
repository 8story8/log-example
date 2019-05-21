#!/bin/bash

TARGET=$1

# update yum
sudo yum update

# install wget
sudo yum install wget -y -q

# install docker
sudo yum install yum-utils device-mapper-persistent-data lvm2 -y
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install docker-ce docker-ce-cli containerd.io -y 
sudo systemctl start docker > /dev/null
sudo setfacl -m u:1000:rw /var/run/docker.sock

# install and execute elasticsearch
MACHINE_IP=$(ip address | grep eth0 | grep inet | awk '{print $2}' | cut -d '/' -f1)
PUBLIC_IP=$(ip address | grep eth1 | grep inet | awk '{print $2}' | cut -d '/' -f1)
ELASTIC_BASE="$TARGET/elasticsearch"

sudo sysctl -w vm.max_map_count=262144

mkdir $ELASTIC_BASE

for ((i=1; i<=3; i++)); do
ELASTIC_CONF="${ELASTIC_BASE}/elasticsearch$i.yml"
cat << EOF >> ${ELASTIC_CONF}
cluster.name: elasticsearch
node.name: node-$i
node.max_local_storage_nodes: 1
network.host: _site_
http.port: 9200
EOF

case $i in
1)
cat >> $ELASTIC_CONF << EOL
transport.tcp.port: 9300
discovery.zen.ping.unicast.hosts: ["$MACHINE_IP:9301", "$MACHINE_IP:9302"]
EOL
docker run -d --name elasticsearch$i -p "9200:9200" -p "9300:9300" -v "$ELASTIC_CONF:/usr/share/elasticsearch/config/elasticsearch.yml" -e "ES_JAVA_OPTS=-Xms256m -Xmx256m" elasticsearch:6.4.0
;;
2)
cat >> $ELASTIC_CONF << EOL
transport.tcp.port: 9301
discovery.zen.ping.unicast.hosts: ["$MACHINE_IP:9300", "$MACHINE_IP:9302"]
EOL
docker run -d --name elasticsearch$i -p "9201:9200" -p "9301:9300" -v "$ELASTIC_CONF:/usr/share/elasticsearch/config/elasticsearch.yml" -e "ES_JAVA_OPTS=-Xms256m -Xmx256m" elasticsearch:6.4.0
;;
3)
cat >> $ELASTIC_CONF << EOL
transport.tcp.port: 9302
discovery.zen.ping.unicast.hosts: ["$MACHINE_IP:9300", "$MACHINE_IP:9301"]
EOL
docker run -d --name elasticsearch$i -p "9202:9200" -p "9302:9300" -v "$ELASTIC_CONF:/usr/share/elasticsearch/config/elasticsearch.yml" -e "ES_JAVA_OPTS=-Xms256m -Xmx256m" elasticsearch:6.4.0
;;
esac
done

# install and execute logstash
LOGSTASH_BASE="$TARGET/logstash"
LOGSTASH_CONF="${LOGSTASH_BASE}/logstash.yml"
LOGSTASH_PIPELINE_CONF="${LOGSTASH_BASE}/logstash.conf"

mkdir $LOGSTASH_BASE

cat << EOF >> ${LOGSTASH_CONF}
xpack.monitoring.enabled: false
EOF

sudo chown 1000:1000 $LOGSTASH_CONF

cat << EOF >> ${LOGSTASH_PIPELINE_CONF}
input {
  tcp {
    port => 4560
  }
}
filter {
  if "[EVENT]" not in [message] {
    drop { }
  }
  grok {
    match => { "message" => "%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} %{GREEDYDATA:message}" }
    overwrite => [ "message" ]
  }
  date {
    match => [ "timestamp", "ISO8601" ]
    target => "@timestamp"
    timezone => "Asia/Seoul"
    remove_field => [ "timestamp" ]
  }
}
output {
  elasticsearch {
    hosts => ["$PUBLIC_IP:9200"]
    index => "event-%{+YYYY.MM.dd}"
  }
}
EOF

sudo chown 1000:1000 $LOGSTASH_PIPELINE_CONF

docker run -d --name logstash -p "4560:4560" -v "$LOGSTASH_CONF:/usr/share/logstash/config/logstash.yml" -v "$LOGSTASH_PIPELINE_CONF:/usr/share/logstash/pipeline/logstash.conf" docker.elastic.co/logstash/logstash:6.4.0

# install and execute kibana
KIBANA_BASE="$TARGET/kibana"
KIBANA_CONF="${KIBANA_BASE}/kibana.yml"

mkdir $KIBANA_BASE

cat << EOF >> ${KIBANA_CONF}
server.port: 5601
server.host: "0.0.0.0"
elasticsearch.url: "http://$PUBLIC_IP:9200"
EOF

sudo chown 1000:1000 $KIBANA_CONF

docker run -d --name kibana -p "5601:5601" -v "$KIBANA_CONF:/usr/share/kibana/config/kibana.yml" docker.elastic.co/kibana/kibana:6.4.0

# install and execute grafana
GRAFANA_BASE="$TARGET/grafana"
GRAFANA_CONF="${GRAFANA_BASE}/grafana.ini"
GRAFANA_DATASOURCE_CONF="${GRAFANA_BASE}/datasource.yaml"
GRAFANA_DASHBOARD_CONF="${GRAFANA_BASE}/dashboard.yaml"

mkdir $GRAFANA_BASE

cat << EOF >> ${GRAFANA_CONF}
[server]
protocol = http
http_port = 3000

[auth.anonymous]
enabled = true

[users]
default_theme = light
EOF

cat << EOF >> ${GRAFANA_DATASOURCE_CONF}
apiVersion: 1

datasources:
  - name: Elasticsearch
    type: elasticsearch
    access: proxy
    database: "*"
    url: http://$PUBLIC_IP:9200
    jsonData:
      timeField: "@timestamp"
      esVersion: 60
    isDefault: true
EOF

cat << EOF >> ${GRAFANA_DASHBOARD_CONF}
apiVersion: 1

providers:
- name: 'default'
  org_id: 1
  folder: ''
  folderUid: ''
  type: file
  updateIntervalSeconds: 10
  options:
    path: /var/lib/grafana/dashboards
EOF

docker run -d --name grafana -p "3000:3000" \
  -v "$GRAFANA_CONF:/etc/grafana/grafana.ini" \
  -v "$GRAFANA_DATASOURCE_CONF:/etc/grafana/provisioning/datasources/datasource.yaml" \
  -v "$GRAFANA_DASHBOARD_CONF:/etc/grafana/provisioning/dashboards/dashboard.yaml" \
  -v "/vagrant/dashboards:/var/lib/grafana/dashboards" \
  grafana/grafana:6.1.4

METRIC_BASE="$TARGET/metricbeat"
METRIC_CONF="${METRIC_BASE}/metricbeat.yml"

mkdir $METRIC_BASE

cat << EOF >> ${METRIC_CONF}
metricbeat.modules:
- module: system
  metricsets:
    - cpu
    - load
    - filesystem
    - diskio
    - fsstat
    - memory
    - network
    - process
  enabled: true
  period: 10s
  processes: ['.*']

- module: docker
  metricsets:
    - "container"
    - "cpu"
    - "diskio"
    - "healthcheck"
    - "info"
    - "image"
    - "memory"
    - "network"
  hosts: ["unix:///var/run/docker.sock"]
  period: 10s
  enabled: true

name: test
 
output.elasticsearch:
  hosts: ["$PUBLIC_IP:9200"]
  template.name: "metricbeat"
  template.path: "metricbeat.template.json"
  template.overwrite: true

setup.kibana:
  host: "$PUBLIC_IP:5601"

setup.dashboards.enabled: true
EOF

sudo chown 1000:1000 $METRIC_CONF

# install and execute metricbeat
sleep 60
docker run -d --net="host" --name metricbeat \
  --mount type=bind,source=$METRIC_CONF,target=/usr/share/metricbeat/metricbeat.yml \
  --mount type=bind,source=/proc,target=/hostfs/proc,readonly \
  --mount type=bind,source=/sys/fs/cgroup,target=/hostfs/sys/fs/cgroup,readonly \
  --mount type=bind,source=/,target=/hostfs,readonly \
  --mount type=bind,source=/var/run/docker.sock,target=/var/run/docker.sock,readonly \
  docker.elastic.co/beats/metricbeat:6.4.0 -system.hostfs=/hostfs
