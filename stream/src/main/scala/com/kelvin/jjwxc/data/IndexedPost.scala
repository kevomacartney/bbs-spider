package com.kelvin.jjwxc.data

import java.util.UUID

case class IndexedPost(
    id: UUID,
    category: String,
    postSubject: String,
    postUrl: String,
    originalPost: String,
    numberOfReplies: Int,
    comments: List[Comment]
)
