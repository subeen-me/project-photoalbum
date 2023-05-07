package com.squarecross.photoalbum.service;

import com.squarecross.photoalbum.Constants;
import com.squarecross.photoalbum.domain.Album;
import com.squarecross.photoalbum.domain.Photo;
import com.squarecross.photoalbum.dto.AlbumDto;
import com.squarecross.photoalbum.repository.AlbumRepository;
import com.squarecross.photoalbum.repository.PhotoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AlbumServiceTest {

    @Autowired
    AlbumRepository albumRepository;

    @Autowired
    AlbumService albumService;
    @Autowired
    private PhotoRepository photoRepository;

    @DisplayName("AlbumId로 앨범을 단건 조회하면, 앨범을 반환한다.")
    @Test
    void 앨범_단건_조회_테스트() {
        Album album = new Album();
        album.setAlbumName("테스트");
        Album savedAlbum = albumRepository.save(album);

        AlbumDto resAlbum = albumService.getAlbum(savedAlbum.getAlbumId());
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

    @DisplayName("AlbumDto의 Count값을 반환한다.")
    @Test
    void 앨범_Count값_반환_테스트() {
        Album album = new Album();
        album.setAlbumName("테스트");
        Album savedAlbum = albumRepository.save(album);

        //사진을 생성하고, setAlbum 을 통해 앨범을 지정해준 이후, repository 에 사진을 저장한다
        Photo photo1 = new Photo();
        Photo photo2 = new Photo();
        photo1.setFileName("사진1");
        photo2.setFileName("사진2");
        photo1.setAlbum(savedAlbum);
        photo2.setAlbum(savedAlbum);

        photoRepository.save(photo1);
        photoRepository.save(photo2);

        AlbumDto resAlbum = albumService.getAlbum(savedAlbum.getAlbumId());
        assertEquals("테스트", resAlbum.getAlbumName());
        assertEquals(2,resAlbum.getCount());

    }

    @DisplayName("앨범명을 입력하면, 앨범이 생성된다.")
    @Test
    void 앨범_생성_테스트() throws IOException {
        AlbumDto albumDto = new AlbumDto();
        albumDto.setAlbumName("New album1");

        AlbumDto savedAlbum = albumService.createAlbum(albumDto);

        assertEquals("New album1", savedAlbum.getAlbumName());
        org.assertj.core.api.Assertions.assertThat(Files.exists(Paths.get(Constants.PATH_PREFIX + "/photos/original/" + savedAlbum.getAlbumId())));
        org.assertj.core.api.Assertions.assertThat(Files.exists(Paths.get(Constants.PATH_PREFIX + "/photos/thumb/" + savedAlbum.getAlbumId())));

        Files.deleteIfExists(Paths.get(Constants.PATH_PREFIX + "/photos/original/" + savedAlbum.getAlbumId()));
        Files.deleteIfExists(Paths.get(Constants.PATH_PREFIX + "/photos/thumb/" + savedAlbum.getAlbumId()));
    }

    @Test
    void 앨범_검색_리스트_정렬_테스트() throws InterruptedException {
        Album album1 = new Album();
        Album album2 = new Album();
        album1.setAlbumName("aaaa");
        album2.setAlbumName("aaab");

        albumRepository.save(album1);
        TimeUnit.SECONDS.sleep(1); // 시간차를 두기 위해 두번째 앨범 생성 1초 딜레이
        albumRepository.save(album2);

        //최신순 정렬, 두번째로 생성한 앨범이 먼저 나와야 한다
        List<Album> resDate = albumRepository.findByAlbumNameContainingOrderByCreatedAtDesc("aaa");
        assertEquals("aaab", resDate.get(0).getAlbumName()); //0번째 Index가 두번째 앨범명 aaab인지 체크
        assertEquals("aaaa", resDate.get(1).getAlbumName()); //1번째 Index가 첫번째 앨범명 aaaa인지 체크
        assertEquals(2, resDate.size());

        //앨범명 정렬, aaaa -> bbbb 기준으로 나와야 한다
        List<Album> resName = albumRepository.findByAlbumNameContainingOrderByAlbumNameAsc("aaa");
        assertEquals("aaaa", resName.get(0).getAlbumName()); //0번째 Index가 첫번째 앨범명 aaaa인지 체크
        assertEquals("aaab", resName.get(1).getAlbumName()); //1번째 Index가 두번째 앨범명 aaab인지 체크
        assertEquals(2, resDate.size());

    }
}