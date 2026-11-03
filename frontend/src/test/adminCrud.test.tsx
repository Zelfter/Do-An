/**
 * Admin CRUD UI tests — Vitest + React Testing Library
 *
 * Verifies CRUD operations for Topics, Questions, Documents, and Users pages.
 * API modules are mocked (vi.spyOn) — no real network calls (architecture guardrail).
 * NFR-26: assertions verify Vietnamese UI text is present.
 */
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as topicApi from '../api/topicApi'
import * as questionApi from '../api/questionApi'
import * as documentApi from '../api/documentApi'
import * as userApi from '../api/userApi'
import type { TopicDto } from '../api/topicApi'
import type { QuestionSummaryDto } from '../api/questionApi'
import type { DocumentSummaryDto } from '../api/documentApi'
import type { UserDto } from '../api/userApi'
import { AuthProvider } from '../contexts/AuthContext'
import * as authApi from '../api/authApi'
import TopicsPage from '../pages/admin/TopicsPage'
import QuestionsPage from '../pages/admin/QuestionsPage'
import DocumentsPage from '../pages/admin/DocumentsPage'
import UsersPage from '../pages/admin/UsersPage'

// ─── Test fixtures ────────────────────────────────────────────────────────────

const topicFixture: TopicDto = {
  id: 't1',
  code: 'java_core',
  parentId: null,
  isActive: true,
  translations: [{ language: 'vi', name: 'Java cơ bản', description: 'Nhập môn Java' }],
  aliases: [{ id: 'a1', alias: 'core java' }],
}

const questionFixture: QuestionSummaryDto = {
  id: 'q1',
  topicId: 't1',
  language: 'vi',
  level: 'JUNIOR',
  content: 'Sự khác biệt giữa abstract class và interface?',
  status: 'DRAFT',
  indexStatus: 'NOT_INDEXED',
}

const documentFixture: DocumentSummaryDto = {
  id: 'd1',
  topicId: 't1',
  language: 'vi',
  title: 'Hướng dẫn OOP trong Java',
  status: 'DRAFT',
  createdAt: '2026-10-16T10:00:00Z',
  updatedAt: '2026-10-16T10:00:00Z',
}

const userFixture: UserDto = {
  id: 'u1',
  email: 'admin@test.com',
  role: 'ADMIN',
  status: 'ACTIVE',
}

// ─── Setup / Teardown ─────────────────────────────────────────────────────────

beforeEach(() => {
  sessionStorage.clear()
  vi.restoreAllMocks()
})

afterEach(() => {
  sessionStorage.clear()
})

// ─── TopicsPage ───────────────────────────────────────────────────────────────

describe('TopicsPage', () => {
  it('renders the list of topics with Vietnamese labels', async () => {
    vi.spyOn(topicApi, 'listTopics').mockResolvedValue([topicFixture])

    render(<TopicsPage />)

    expect(await screen.findByText('java_core')).toBeInTheDocument()
    expect(screen.getByText('Java cơ bản')).toBeInTheDocument()
    expect(screen.getByText('Quản lý chủ đề')).toBeInTheDocument()
  })

  it('opens create form when "Thêm chủ đề" is clicked', async () => {
    vi.spyOn(topicApi, 'listTopics').mockResolvedValue([])
    const user = userEvent.setup()

    render(<TopicsPage />)
    await screen.findByText('Chưa có chủ đề nào.')

    await user.click(screen.getByRole('button', { name: /\+ thêm chủ đề/i }))

    expect(await screen.findByRole('dialog', { name: /thêm chủ đề/i })).toBeInTheDocument()
    expect(screen.getByLabelText(/mã \(snake_case/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/tên \(tiếng việt\)/i)).toBeInTheDocument()
  })

  it('creates a topic via the form and calls createTopic', async () => {
    vi.spyOn(topicApi, 'listTopics').mockResolvedValue([])
    const createSpy = vi.spyOn(topicApi, 'createTopic').mockResolvedValue(topicFixture)
    const user = userEvent.setup()

    render(<TopicsPage />)
    await screen.findByText('Chưa có chủ đề nào.')

    await user.click(screen.getByRole('button', { name: /\+ thêm chủ đề/i }))

    await user.type(screen.getByLabelText(/mã \(snake_case/i), 'java_core')
    await user.type(screen.getByLabelText(/tên \(tiếng việt\)/i), 'Java cơ bản')
    await user.click(screen.getByRole('button', { name: /lưu/i }))

    await waitFor(() => expect(createSpy).toHaveBeenCalledTimes(1))
    expect(createSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        code: 'java_core',
        translations: expect.arrayContaining([
          expect.objectContaining({ language: 'vi', name: 'Java cơ bản' }),
        ]),
      })
    )
  })

  it('shows Vietnamese error on 409 duplicate code', async () => {
    vi.spyOn(topicApi, 'listTopics').mockResolvedValue([])
    vi.spyOn(topicApi, 'createTopic').mockRejectedValue({ response: { status: 409 } })
    const user = userEvent.setup()

    render(<TopicsPage />)
    await screen.findByText('Chưa có chủ đề nào.')

    await user.click(screen.getByRole('button', { name: /\+ thêm chủ đề/i }))
    await user.type(screen.getByLabelText(/mã \(snake_case/i), 'dup')
    await user.type(screen.getByLabelText(/tên \(tiếng việt\)/i), 'Trùng')
    await user.click(screen.getByRole('button', { name: /lưu/i }))

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent('Mã chủ đề đã tồn tại.')
    )
  })

  it('confirms before deleting a topic and calls deleteTopic', async () => {
    vi.spyOn(topicApi, 'listTopics').mockResolvedValue([topicFixture])
    const deleteSpy = vi.spyOn(topicApi, 'deleteTopic').mockResolvedValue(undefined)
    const user = userEvent.setup()

    render(<TopicsPage />)
    await screen.findByText('java_core')

    await user.click(screen.getByRole('button', { name: /^xóa$/i }))

    // Confirm dialog should appear with Vietnamese text
    const confirmDialog = await screen.findByRole('dialog', { name: /xóa chủ đề/i })
    expect(confirmDialog).toHaveTextContent('không thể hoàn tác')

    // Click the confirm button inside the dialog (scoped to avoid table's Xóa button)
    await user.click(within(confirmDialog).getByRole('button', { name: /^xóa$/i }))

    await waitFor(() => expect(deleteSpy).toHaveBeenCalledWith('t1'))
    await waitFor(() => expect(screen.getByText('Đã xóa chủ đề.')).toBeInTheDocument())
  })

  it('shows Vietnamese error on 409 delete (linked questions)', async () => {
    vi.spyOn(topicApi, 'listTopics').mockResolvedValue([topicFixture])
    vi.spyOn(topicApi, 'deleteTopic').mockRejectedValue({ response: { status: 409 } })
    const user = userEvent.setup()

    render(<TopicsPage />)
    await screen.findByText('java_core')

    await user.click(screen.getByRole('button', { name: /^xóa$/i }))
    const dialog = await screen.findByRole('dialog', { name: /xóa chủ đề/i })
    await user.click(within(dialog).getByRole('button', { name: /^xóa$/i }))

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent('câu hỏi hoặc tài liệu liên kết')
    )
  })

  it('opens alias management modal and adds an alias', async () => {
    vi.spyOn(topicApi, 'listTopics').mockResolvedValue([topicFixture])
    vi.spyOn(topicApi, 'listAliases').mockResolvedValue([{ id: 'a1', alias: 'core java' }])
    const addAliasSpy = vi.spyOn(topicApi, 'addAlias').mockResolvedValue({ id: 'a2', alias: 'java cơ bản' })
    const user = userEvent.setup()

    render(<TopicsPage />)
    await screen.findByText('java_core')

    await user.click(screen.getByRole('button', { name: /alias/i }))

    expect(await screen.findByText('core java')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('Alias mới')).toBeInTheDocument()

    await user.type(screen.getByPlaceholderText('Alias mới'), 'java cơ bản')
    await user.click(screen.getByRole('button', { name: /^thêm$/i }))

    await waitFor(() => expect(addAliasSpy).toHaveBeenCalledWith('t1', 'java cơ bản'))
  })
})

// ─── QuestionsPage ───────────────────────────────────────────────────────────

describe('QuestionsPage', () => {
  it('renders the question list with Vietnamese labels', async () => {
    vi.spyOn(questionApi, 'listQuestions').mockResolvedValue([questionFixture])
    vi.spyOn(topicApi, 'listTopics').mockResolvedValue([topicFixture])

    render(<QuestionsPage />)

    expect(await screen.findByText('Sự khác biệt giữa abstract class và interface?')).toBeInTheDocument()
    expect(screen.getByText('Ngân hàng câu hỏi')).toBeInTheDocument()
    expect(screen.getByText('Bản nháp')).toBeInTheDocument()
  })

  it('shows 422 missing fields error when publish fails (AC-Q-01)', async () => {
    vi.spyOn(questionApi, 'listQuestions').mockResolvedValue([questionFixture])
    vi.spyOn(topicApi, 'listTopics').mockResolvedValue([topicFixture])
    vi.spyOn(questionApi, 'publishQuestion').mockRejectedValue({
      response: { status: 422, data: { missingFields: ['reference_answer', 'key_points'] } },
    })
    const user = userEvent.setup()

    render(<QuestionsPage />)
    await screen.findByText('Sự khác biệt giữa abstract class và interface?')

    await user.click(screen.getByRole('button', { name: /xuất bản/i }))

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent('reference_answer, key_points')
    )
  })

  it('calls publishQuestion and shows success on publish', async () => {
    vi.spyOn(questionApi, 'listQuestions').mockResolvedValue([questionFixture])
    vi.spyOn(topicApi, 'listTopics').mockResolvedValue([topicFixture])
    const publishSpy = vi.spyOn(questionApi, 'publishQuestion').mockResolvedValue({
      id: 'q1',
      status: 'PUBLISHED',
      indexStatus: 'INDEXING',
      indexingTriggered: true,
    })
    const user = userEvent.setup()

    render(<QuestionsPage />)
    await screen.findByText('Sự khác biệt giữa abstract class và interface?')

    await user.click(screen.getByRole('button', { name: /xuất bản/i }))

    await waitFor(() => expect(publishSpy).toHaveBeenCalledWith('q1'))
    await waitFor(() => expect(screen.getByText('Đã xuất bản câu hỏi.')).toBeInTheDocument())
  })

  it('archives a question and shows success', async () => {
    vi.spyOn(questionApi, 'listQuestions').mockResolvedValue([questionFixture])
    vi.spyOn(topicApi, 'listTopics').mockResolvedValue([topicFixture])
    const archiveSpy = vi.spyOn(questionApi, 'archiveQuestion').mockResolvedValue({
      ...questionFixture,
      referenceAnswer: null,
      keyPoints: [],
      status: 'ARCHIVED',
    })
    const user = userEvent.setup()

    render(<QuestionsPage />)
    await screen.findByText('Sự khác biệt giữa abstract class và interface?')

    await user.click(screen.getByRole('button', { name: /lưu trữ/i }))

    await waitFor(() => expect(archiveSpy).toHaveBeenCalledWith('q1'))
    await waitFor(() => expect(screen.getByText('Đã lưu trữ câu hỏi.')).toBeInTheDocument())
  })

  it('creates a question via the form', async () => {
    vi.spyOn(questionApi, 'listQuestions').mockResolvedValue([])
    vi.spyOn(topicApi, 'listTopics').mockResolvedValue([topicFixture])
    const createSpy = vi.spyOn(questionApi, 'createQuestion').mockResolvedValue({
      ...questionFixture,
      referenceAnswer: null,
      keyPoints: [],
    })
    const user = userEvent.setup()

    render(<QuestionsPage />)
    await screen.findByText('Chưa có câu hỏi nào.')

    await user.click(screen.getByRole('button', { name: /\+ thêm câu hỏi/i }))

    await user.type(screen.getByLabelText(/nội dung câu hỏi/i), 'Comparator vs Comparable?')
    await user.click(screen.getByRole('button', { name: /lưu/i }))

    await waitFor(() => expect(createSpy).toHaveBeenCalledTimes(1))
    expect(createSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        topicId: 't1',
        level: 'JUNIOR',
        content: 'Comparator vs Comparable?',
      })
    )
  })
})

// ─── DocumentsPage ───────────────────────────────────────────────────────────

describe('DocumentsPage', () => {
  it('renders the document list with Vietnamese labels', async () => {
    vi.spyOn(documentApi, 'listDocuments').mockResolvedValue([documentFixture])
    vi.spyOn(topicApi, 'listTopics').mockResolvedValue([topicFixture])

    render(<DocumentsPage />)

    expect(await screen.findByText('Hướng dẫn OOP trong Java')).toBeInTheDocument()
    expect(screen.getByText('Quản lý tài liệu')).toBeInTheDocument()
    expect(screen.getByText('Bản nháp')).toBeInTheDocument()
  })

  it('creates a document via the form', async () => {
    vi.spyOn(documentApi, 'listDocuments').mockResolvedValue([])
    vi.spyOn(topicApi, 'listTopics').mockResolvedValue([topicFixture])
    const createSpy = vi.spyOn(documentApi, 'createDocument').mockResolvedValue({
      id: 'd1',
      topicId: 't1',
      language: 'vi',
      title: 'Tài liệu mới',
      contentMd: '# Nội dung',
      status: 'DRAFT',
      indexError: null,
      createdAt: '2026-10-16T10:00:00Z',
      updatedAt: '2026-10-16T10:00:00Z',
    })
    const user = userEvent.setup()

    render(<DocumentsPage />)
    await screen.findByText('Chưa có tài liệu nào.')

    await user.click(screen.getByRole('button', { name: /\+ thêm tài liệu/i }))

    await user.type(screen.getByLabelText(/tiêu đề/i), 'Tài liệu mới')
    await user.type(screen.getByLabelText(/nội dung \(markdown\)/i), '# Nội dung')
    await user.click(screen.getByRole('button', { name: /lưu/i }))

    await waitFor(() => expect(createSpy).toHaveBeenCalledTimes(1))
    expect(createSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        topicId: 't1',
        title: 'Tài liệu mới',
        contentMd: '# Nội dung',
      })
    )
  })

  it('reindexes a document and shows success when INDEXED', async () => {
    vi.spyOn(documentApi, 'listDocuments').mockResolvedValue([documentFixture])
    vi.spyOn(topicApi, 'listTopics').mockResolvedValue([topicFixture])
    const reindexSpy = vi.spyOn(documentApi, 'reindexDocument').mockResolvedValue({
      documentId: 'd1',
      status: 'INDEXED',
      indexError: null,
    })
    const user = userEvent.setup()

    render(<DocumentsPage />)
    await screen.findByText('Hướng dẫn OOP trong Java')

    await user.click(screen.getByRole('button', { name: /đánh chỉ mục lại/i }))

    await waitFor(() => expect(reindexSpy).toHaveBeenCalledWith('d1'))
    await waitFor(() => expect(screen.getByText('Đã đánh chỉ mục lại thành công.')).toBeInTheDocument())
  })

  it('shows error when delete fails with 409 (referenced)', async () => {
    vi.spyOn(documentApi, 'listDocuments').mockResolvedValue([documentFixture])
    vi.spyOn(topicApi, 'listTopics').mockResolvedValue([topicFixture])
    vi.spyOn(documentApi, 'deleteDocument').mockRejectedValue({ response: { status: 409 } })
    const user = userEvent.setup()

    render(<DocumentsPage />)
    await screen.findByText('Hướng dẫn OOP trong Java')

    await user.click(screen.getByRole('button', { name: /^xóa$/i }))
    const dialog = await screen.findByRole('dialog', { name: /xóa tài liệu/i })
    await user.click(within(dialog).getByRole('button', { name: /^xóa$/i }))

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent('đang được tham chiếu')
    )
  })
})

// ─── UsersPage ───────────────────────────────────────────────────────────────

describe('UsersPage', () => {
  // The authenticated admin (current user) — id differs from list fixtures below
  // so the lock/unlock buttons are NOT disabled.
  const currentUser: UserDto = {
    id: 'admin-0',
    email: 'admin@test.com',
    role: 'ADMIN',
    status: 'ACTIVE',
  }

  function renderUsersPage() {
    vi.spyOn(authApi, 'getMe').mockResolvedValue(currentUser)
    authApi.tokenStorage.setTokens('tok', 'rtok')
    return render(
      <AuthProvider>
        <UsersPage />
      </AuthProvider>
    )
  }

  it('renders the user list with Vietnamese labels', async () => {
    vi.spyOn(userApi, 'listUsers').mockResolvedValue([userFixture])

    renderUsersPage()

    expect(await screen.findByText('admin@test.com')).toBeInTheDocument()
    expect(screen.getByText('Quản lý người dùng')).toBeInTheDocument()
  })

  it('locks a user and calls updateUserStatus', async () => {
    // Use a user with a different id so the lock button is not disabled
    const targetUser: UserDto = { ...userFixture, id: 'u2', email: 'other@test.com' }
    vi.spyOn(userApi, 'listUsers').mockResolvedValue([targetUser])
    const updateSpy = vi.spyOn(userApi, 'updateUserStatus').mockResolvedValue({
      ...targetUser,
      status: 'LOCKED',
    })
    const user = userEvent.setup()

    renderUsersPage()
    await screen.findByText('other@test.com')

    await user.click(screen.getByRole('button', { name: /khóa/i }))

    await waitFor(() => expect(updateSpy).toHaveBeenCalledWith('u2', 'LOCKED'))
    await waitFor(() => expect(screen.getByText(/Đã khóa tài khoản/)).toBeInTheDocument())
  })

  it('unlocks a locked user', async () => {
    const lockedUser: UserDto = { ...userFixture, id: 'u2', email: 'other@test.com', status: 'LOCKED' }
    vi.spyOn(userApi, 'listUsers').mockResolvedValue([lockedUser])
    const updateSpy = vi.spyOn(userApi, 'updateUserStatus').mockResolvedValue({
      ...lockedUser,
      status: 'ACTIVE',
    })
    const user = userEvent.setup()

    renderUsersPage()
    await screen.findByText('other@test.com')

    await user.click(screen.getByRole('button', { name: /mở khóa/i }))

    await waitFor(() => expect(updateSpy).toHaveBeenCalledWith('u2', 'ACTIVE'))
  })

  it('shows self-lock error (400) in Vietnamese', async () => {
    // Use a non-self user so the button is clickable; mock API to return 400
    // to verify the error message handling for the self-lock status code.
    const targetUser: UserDto = { ...userFixture, id: 'u2', email: 'other@test.com' }
    vi.spyOn(userApi, 'listUsers').mockResolvedValue([targetUser])
    vi.spyOn(userApi, 'updateUserStatus').mockRejectedValue({ response: { status: 400 } })
    const user = userEvent.setup()

    renderUsersPage()
    await screen.findByText('other@test.com')

    await user.click(screen.getByRole('button', { name: /khóa/i }))

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent('Không thể khóa chính tài khoản của bạn.')
    )
  })
})

// ─── NFR-26: Vietnamese UI labels for enum values ─────────────────────────────
//
// Verifies that enum values (level, index-status, role) shown to users are
// rendered in Vietnamese, consistent with the existing STATUS_LABELS translations.

describe('NFR-26: Vietnamese labels for enum values', () => {
  it('QuestionsPage renders Vietnamese level and index-status labels', async () => {
    vi.spyOn(questionApi, 'listQuestions').mockResolvedValue([questionFixture])
    vi.spyOn(topicApi, 'listTopics').mockResolvedValue([topicFixture])

    render(<QuestionsPage />)
    await screen.findByText('Sự khác biệt giữa abstract class và interface?')

    // Level label for JUNIOR → "Sơ cấp"
    expect(screen.getByText('Sơ cấp')).toBeInTheDocument()
    // Index-status label for NOT_INDEXED → "Chưa đánh chỉ mục"
    expect(screen.getByText('Chưa đánh chỉ mục')).toBeInTheDocument()
    // Column header translated from English "Index" → "Chỉ mục"
    expect(screen.getByText('Chỉ mục')).toBeInTheDocument()
  })

  it('UsersPage renders Vietnamese role label instead of raw enum', async () => {
    vi.spyOn(authApi, 'getMe').mockResolvedValue({
      id: 'admin-0',
      email: 'admin@test.com',
      role: 'ADMIN',
      status: 'ACTIVE',
    })
    authApi.tokenStorage.setTokens('tok', 'rtok')
    vi.spyOn(userApi, 'listUsers').mockResolvedValue([userFixture])

    render(
      <AuthProvider>
        <UsersPage />
      </AuthProvider>
    )
    await screen.findByText('admin@test.com')

    // Role label for ADMIN → "Quản trị" (appears in table cell and filter dropdown)
    expect(screen.getAllByText('Quản trị').length).toBeGreaterThanOrEqual(1)
    // Raw English enum value must NOT be displayed in the table body
    expect(screen.queryByText('ADMIN')).not.toBeInTheDocument()
  })
})
