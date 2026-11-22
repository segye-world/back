package com.segye.category;

import com.segye.account.AccountRecordRepository;
import com.segye.category.dto.CategoryDtos;
import com.segye.member.Member;
import com.segye.member.MemberRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryService {

    private static final List<String> DEFAULT_INCOME_CATEGORIES = List.of("월급", "현금", "부수입");
    private static final String CATEGORY_IN_USE = "수입·지출 기록에서 사용 중인 카테고리는 삭제할 수 없습니다.";

    private final CategoryRepository repo;
    private final MemberRepository memberRepo;
    private final AccountRecordRepository accountRecordRepo;

    public CategoryService(CategoryRepository repo, MemberRepository memberRepo, AccountRecordRepository accountRecordRepo) {
        this.repo = repo;
        this.memberRepo = memberRepo;
        this.accountRecordRepo = accountRecordRepo;
    }

    @Transactional(readOnly = true)
    public List<CategoryDtos.CategoryResponse> list(Long memberId, CategoryType type) {
        List<Category> categories = type == null
                ? repo.findByMember_IdOrderById(memberId)
                : repo.findByMember_IdAndTypeOrderById(memberId, type);
        return categories.stream()
                .map(this::toResponse)
                .toList();
    }

    public List<Category> createDefaultCategories(Member member) {
        return repo.saveAll(DEFAULT_INCOME_CATEGORIES.stream()
                .map(name -> new Category(member, name, CategoryType.INCOME))
                .toList());
    }

    public CategoryDtos.CategoryResponse create(Long memberId, CategoryDtos.UpsertRequest req) {
        Member member = memberRepo.findById(memberId).orElseThrow(() -> new SecurityException("회원이 없습니다."));
        return toResponse(repo.save(new Category(member, req.getName(), req.getType())));
    }

    public CategoryDtos.CategoryResponse update(Long memberId, Long id, CategoryDtos.UpsertRequest req) {
        Category category = findOwnedCategory(memberId, id);
        category.update(req.getName(), req.getType());
        return toResponse(category);
    }

    public void delete(Long memberId, Long id) {
        Category category = findOwnedCategory(memberId, id);
        if (accountRecordRepo.existsByCategory_Id(id)) {
            throw new IllegalArgumentException(CATEGORY_IN_USE);
        }
        try {
            repo.delete(category);
            repo.flush();
        } catch (DataIntegrityViolationException e) {
            // 삭제 요청과 동시에 거래가 저장돼도 기본 카테고리 참조는 FK가 보호한다.
            throw new IllegalArgumentException(CATEGORY_IN_USE, e);
        }
    }

    private Category findOwnedCategory(Long memberId, Long id) {
        return repo.findByIdAndMember_Id(id, memberId)
                .orElseThrow(() -> new IllegalArgumentException("카테고리가 없습니다."));
    }

    private CategoryDtos.CategoryResponse toResponse(Category category) {
        return new CategoryDtos.CategoryResponse(category.getId(), category.getName(), category.getType());
    }
}
