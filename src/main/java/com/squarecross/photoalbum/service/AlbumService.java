package com.squarecross.photoalbum.service;

import com.squarecross.photoalbum.domain.Album;
import com.squarecross.photoalbum.repository.AlbumRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.util.Optional;

@Service
public class AlbumService {

    @Autowired
    private AlbumRepository albumRepository;

    public Album getAlbum(Long albumId) {
        Optional<Album> res = albumRepository.findById(albumId);
        if(res.isPresent()) {
            return res.get();
        } else {
            throw new EntityNotFoundException(String.format("앨범 아이디 %d으로 조회되지 않았습니다", albumId));
        }
    }

    public Album searchAlbum(String searchKeyword) {
        if(searchKeyword == null || searchKeyword.isBlank()) {
            throw new EntityNotFoundException("검색어가 입력되지 않앗습니다.");
        }
        Optional<Album> findAlbum = albumRepository.findByAlbumNameContainingIgnoreCase(searchKeyword);
        if (findAlbum.isPresent()) {
            return findAlbum.get();
        } else {
            throw new EntityNotFoundException(String.format("%d로 조회된 결과가 없습니다", searchKeyword));
        }

    }
}
