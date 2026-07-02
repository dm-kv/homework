package ru.netology.nmedia.repository

import androidx.lifecycle.*
import okio.IOException
import ru.netology.nmedia.api.*
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.toDto
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.NetworkError
import ru.netology.nmedia.error.UnknownError

class PostRepositoryImpl(private val dao: PostDao) : PostRepository {
    override val data = dao.getAll().map(List<PostEntity>::toDto)

    override suspend fun getAll() {
        try {
            val response = PostsApi.service.getAll()
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(body.toEntity())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun save(post: Post) {
        try {
            val response = PostsApi.service.save(post)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(PostEntity.fromDto(body))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun likeById(id: Long) {
        val current = dao.getById(id) ?: throw IllegalStateException("Post not found locally")
        val isLiked = current.likedByMe

        try {
            val response = if (isLiked) {
                PostsApi.service.dislikeById(id)
            } else {
                PostsApi.service.likeById(id)
            }
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val updated = current.copy(
                likedByMe = !isLiked,
                likes = if (isLiked) (current.likes ?: 0) - 1 else (current.likes ?: 0) + 1
            )
            dao.insert(updated)

        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun removeById(id: Long) {
        val removedPost = dao.getById(id)
            ?: throw IllegalStateException("Post not found locally")

        dao.removeById(id)

        try {
            val response = PostsApi.service.removeById(id)
            if (!response.isSuccessful) {
                dao.insert(removedPost)
                throw ApiError(response.code(), response.message())
            }
        } catch (e: IOException) {
            dao.insert(removedPost)
            throw NetworkError
        } catch (e: Exception) {
            dao.insert(removedPost)
            throw UnknownError
        }
    }
}

