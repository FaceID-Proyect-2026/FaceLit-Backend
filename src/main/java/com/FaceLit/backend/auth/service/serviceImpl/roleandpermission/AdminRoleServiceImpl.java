package com.FaceLit.backend.auth.service.serviceImpl.roleandpermission;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.FaceLit.backend.auth.repository.roleandpermission.RoleRepository;
import com.FaceLit.backend.auth.repository.roleandpermission.UserRoleRepository;
import com.FaceLit.backend.auth.repository.security.CredentialRepository;
import com.FaceLit.backend.auth.repository.security.UserRepository;
import com.FaceLit.backend.auth.service.roleandpermission.AdminRoleService;

@Service
public class AdminRoleServiceImpl implements AdminRoleService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final CredentialRepository credentialRepository;

    public AdminRoleServiceImpl (
        UserRepository userRepository, 
        UserRoleRepository userRoleRepository,
        RoleRepository roleRepository,
        CredentialRepository credentialRepository  ) {

        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.credentialRepository = credentialRepository;
        }

        @Override
        public List<UserListResponseDTO> getAllUsers()  {




        }

  


}
