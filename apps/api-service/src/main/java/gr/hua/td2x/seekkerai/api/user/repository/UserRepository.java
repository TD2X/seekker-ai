package gr.hua.td2x.seekkerai.api.user.repository;

import gr.hua.td2x.seekkerai.api.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}
