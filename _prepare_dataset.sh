#!/bin/sh
set -ex

which lbzip2 || sudo apt-get install --no-install-recommends lbzip2
which swift || sudo apt-get install --no-install-recommends openstack-clients
which jq || sudo apt-get install --no-install-recommends jq

max_file_size=`wget --quiet https://storage.gra5.cloud.ovh.net/info --output-document - | jq '.swift.max_file_size'`
max_file_size=536870912 # 512MB
max_file_size=134217728 # 128MB

upload_dataset() {
  url="$1"
  echo $url | grep '\.bz2$' || (echo "$url is not a bz2 file"; return)
  archive="`basename $url`"
  ls $archive || wget --continue "$url"
  filename="${archive%.bz2}"
  ls "$filename" || lbunzip2 --keep "$archive"
  swift stat wiki "$archive" || swift upload --use-slo --segment-size ${max_file_size} wiki "$archive"
  swift stat wiki "$filename" || swift upload --use-slo --segment-size ${max_file_size} wiki "$filename"
}

upload_dataset "https://dumps.wikimedia.org/enwiki/20190801/enwiki-20190801-pages-articles-multistream2.xml-p30304p88444.bz2"

upload_dataset "https://dumps.wikimedia.org/enwiki/20190801/enwiki-20190801-pages-articles-multistream.xml.bz2"
