package com.kelvin.jjwxc.data

import com.github.nscala_time.time.Imports._

import java.util.UUID

case class IndexedPost(
    id: UUID,
    category: String,
    postTopic: String,
    postUrl: String,
    postTime: DateTime,
    originalComment: String,
    postOwner: String,
    numberOfReplies: Int,
    comments: List[Comment]
)
