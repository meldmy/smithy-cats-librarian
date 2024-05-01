
$version: "2"

namespace content

use alloy#simpleRestJson

@simpleRestJson
service ContentService {
    operations: [CreateContent, UpdateContent, GetContent, QueryContents]
}

@http(method: "POST", uri: "/contents", code: 201)
operation CreateContent {
    input: ContentInput
    output := {
        @required
        id: ContentId
    }
}

@http(method: "PUT", uri: "/contents/{id}", code: 200)
operation UpdateContent {
    input: UpdateContentInput
    output: Content
}

@http(method: "GET", uri: "/contents/{id}")
@readonly
operation GetContent {
    input := {
        @required
        @httpLabel
        id: ContentId
    }
    output: Content
}

@http(method: "POST", uri: "/contents/query")
@readonly
operation QueryContents {
    input := {
        @required
        input: String
    }
    output := {
        @required
        playable: PlayableList
    }
}

list PlayableList {
    member: Content
}

list PlayableIds {
    member: ContentId
}

@input
structure ContentInput {
    @required
    id: ContentId
    @required
    type: ContentType
    @required
    group: ContentGroup
    @required
    title: ContentTitle
//    TODO: add check for container type only
    containerPlayableIds: PlayableIds
}

@input
structure UpdateContentInput {
    @required
    @httpLabel
    id: ContentId
    @required
    type: ContentType
    @required
    group: ContentGroup
    @required
    title: ContentTitle
    //    TODO: add check for container type only
    containerPlayableIds: PlayableIds
}

structure Content {
    @required
    id: ContentId
    @required
    type: ContentType
    @required
    group: ContentGroup
    @required
    title: ContentTitle
    containerPlayableIds: PlayableIds
}

string ContentId

enum ContentType {
    CHANNEL = "channel"
    EPISODE = "episode"
    SHOW = "show"
    CATEGORY = "category"
}

enum ContentGroup {
    PLAYABLE = "playable"
    CONTAINER = "container"
}

string ContentTitle

@error("client")
@httpError(404)
structure ContentNotFound {
    @required
    message: String
}
