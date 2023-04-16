package com.squarecross.photoalbum.service;

import com.squarecross.photoalbum.domain.Album;
import com.squarecross.photoalbum.repository.AlbumRepository;
import org.hibernate.action.internal.EntityActionVetoException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AlbumServiceTest {

    @Autowired
    AlbumRepository albumRepository;

    @Autowired
    AlbumService albumService;

    @DisplayName("AlbumId로 앨범을 단건 조회하면, 앨범을 반환한다.")
    @Test
    void 앨범_단건_조회_테스트() {
        Album album = new Album();
        album.setAlbumName("테스트");
        Album savedAlbum = albumRepository.save(album);

        Album resAlbum = albumService.getAlbum(savedAlbum.getAlbumId());
        assertEquals("테스트", resAlbum.getAlbumName());
    }

    @DisplayName("AlbumId로 조회가 안 될 경우, 예외를 던진다.")
    @Test
    void AlbumId로_조회_예외_테스트() {
        Long albumId = 0L;

        Throwable exception = assertThrows(EntityNotFoundException.class, () -> {
            albumService.getAlbum(albumId);
        });
        assertEquals("앨범 아이디 " + albumId + "으로 조회되지 않았습니다", exception.getMessage());

    }

    @DisplayName("앨범명으로 검색하면, 앨범을 반환한다.")
    @Test
    void 앨범명_검색_테스트() {
        Album album = new Album();
        album.setAlbumName("테스트");
        albumRepository.save(album);
        String searchKeyword = "테";

        Album findAlbum = albumService.searchAlbum(searchKeyword);
        assertEquals("테스트", findAlbum.getAlbumName());
    }
}