#!/usr/bin/env bash
# Usage: ./bump_version.sh file ,  <major|minor|patch> - Increments the relevant version part by one.
# https://github.com/jfrog/consulting/blob/master/jenkins/helm-app-demo/update_version.sh

set -e

function getProperty {
   PROP_KEY=$1
   PROP_FILE=$2
   PROP_VALUE=`cat $PROP_FILE | grep "$PROP_KEY" | cut -d':' -f2`
   echo $PROP_VALUE
}

function bump_file() {
    sed -i -e "s/$current_version/$new_version/g" $file
}

if [ "$1" == "" ]; then
	echo >&2 "No 'file' set. Aborting."
	exit 1
fi

file=$1

if [ "$2" == "" ]; then
	echo >&2 "No 'type' version set. Aborting."
	exit 1
fi

if [ "$2" == "major" ] || [ "$2" == "minor" ] || [ "$2" == "patch" ]; then
	current_version=$(getProperty version $file)

	IFS='.' read -a version_parts <<< "$current_version"

	major=${version_parts[0]}
	minor=${version_parts[1]}
	patch=${version_parts[2]}

	case "$2" in
		"major")
			major=$((major + 1))
			minor=0
			patch=0
			;;
		"minor")
			minor=$((minor + 1))
			patch=0
			;;
		"patch")
			patch=$((patch + 1))
			;;
	esac
	new_version="$major.$minor.$patch"
fi

#echo $current_version
#echo $new_version
#echo $file

bump_file
echo $new_version
