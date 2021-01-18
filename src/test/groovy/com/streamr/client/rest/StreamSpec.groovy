package com.streamr.client.rest

import com.squareup.moshi.JsonReader
import java.nio.charset.Charset
import okio.Buffer
import spock.lang.Specification

class StreamSpec extends Specification {
    private static JsonReader toReader(String json) {
        return JsonReader.of(new Buffer().writeString(json, Charset.forName("UTF-8")))
    }

    void "Stream objects parse correctly"() {
        String json = "{\"id\":\"7wa7APtlTq6EC5iTCBy6dw\",\"partitions\":1,\"name\":\"Public transport demo\",\"feed\":{\"id\":7,\"name\":\"API\",\"module\":147},\"config\":{\"topic\":\"7wa7APtlTq6EC5iTCBy6dw\",\"fields\":[{\"name\":\"veh\",\"type\":\"string\"},{\"name\":\"spd\",\"type\":\"number\"},{\"name\":\"hdg\",\"type\":\"number\"},{\"name\":\"lat\",\"type\":\"number\"},{\"name\":\"long\",\"type\":\"number\"},{\"name\":\"line\",\"type\":\"string\"}]},\"description\":\"Helsinki tram data by HSL\",\"uiChannel\":false,\"dateCreated\":\"2016-05-27T15:46:30Z\",\"lastUpdated\":\"2017-04-10T16:04:38Z\"}"

        when:
        Stream s = StreamrRESTClient.streamJsonAdapter.fromJson(toReader(json))

        then:
        s.id == "7wa7APtlTq6EC5iTCBy6dw"
        s.name == "Public transport demo"
        s.config.fields.size() == 6
    }
}
