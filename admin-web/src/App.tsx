import { Routes, Route, Navigate } from 'react-router-dom';
import ProtectedRoute from './routes/ProtectedRoute';
import RoleGuard from './routes/RoleGuard';
import AdminLayout from './components/layout/AdminLayout';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import QtPassagesPage from './pages/QtPassagesPage';
import AiAssetsPage from './pages/AiAssetsPage';
import ReportsPage from './pages/ReportsPage';
import PraiseSongsPage from './pages/PraiseSongsPage';
import NoticesPage from './pages/NoticesPage';
import AuditLogsPage from './pages/AuditLogsPage';
import AiMonitoringPage from './pages/AiMonitoringPage';
import AiChecklistsPage from './pages/AiChecklistsPage';
import AiBatchRunLogsPage from './pages/AiBatchRunLogsPage';
import AiEvaluationsPage from './pages/AiEvaluationsPage';
import SimulatorClipsPage from './pages/SimulatorClipsPage';
import AiPromptVersionsPage from './pages/AiPromptVersionsPage';
import NotFoundPage from './pages/NotFoundPage';
import { MENU_ITEMS } from './constants/menu';

const rolesByPath = new Map(MENU_ITEMS.map((item) => [item.path, item.requiredRoles]));

function withRole(path: string, element: React.ReactNode) {
  return (
    <RoleGuard requiredRoles={rolesByPath.get(path) ?? []}>{element}</RoleGuard>
  );
}

// ===== 주소(URL) ↔ 화면 연결 정의 =====
// - /login        : 누구나 접근 (로그인 화면)
// - 그 외 보호 구역 : ProtectedRoute 로 감싸 로그인해야 접근 가능
//                    + AdminLayout(사이드바+헤더) 안에서 그려진다.
// - '/'           : 첫 진입 시 대시보드로 이동
// - '*'           : 정의되지 않은 주소는 404 화면
export default function App() {
  return (
    <Routes>
      {/* 공개 화면 */}
      <Route path="/login" element={<LoginPage />} />

      {/* 보호 화면 (로그인 필요) */}
      <Route element={<ProtectedRoute />}>
        <Route element={<AdminLayout />}>
          <Route
            path="/dashboard"
            element={withRole('/dashboard', <DashboardPage />)}
          />
          <Route
            path="/qt-passages"
            element={withRole('/qt-passages', <QtPassagesPage />)}
          />
          <Route
            path="/ai-assets"
            element={withRole('/ai-assets', <AiAssetsPage />)}
          />
          <Route path="/reports" element={withRole('/reports', <ReportsPage />)} />
          <Route
            path="/praise-songs"
            element={withRole('/praise-songs', <PraiseSongsPage />)}
          />
          <Route path="/notices" element={withRole('/notices', <NoticesPage />)} />
          <Route
            path="/audit-logs"
            element={withRole('/audit-logs', <AuditLogsPage />)}
          />
          <Route
            path="/ai-monitoring"
            element={withRole('/ai-monitoring', <AiMonitoringPage />)}
          />
          <Route
            path="/ai-checklists"
            element={withRole('/ai-checklists', <AiChecklistsPage />)}
          />
          <Route
            path="/ai-batch-logs"
            element={withRole('/ai-batch-logs', <AiBatchRunLogsPage />)}
          />
          <Route
            path="/ai-evaluations"
            element={withRole('/ai-evaluations', <AiEvaluationsPage />)}
          />
          <Route
            path="/simulator-clips"
            element={withRole('/simulator-clips', <SimulatorClipsPage />)}
          />
          <Route
            path="/ai-prompt-versions"
            element={withRole('/ai-prompt-versions', <AiPromptVersionsPage />)}
          />
        </Route>
      </Route>

      {/* 첫 진입 시 대시보드로 보낸다 */}
      <Route path="/" element={<Navigate to="/dashboard" replace />} />

      {/* 정의되지 않은 모든 주소 */}
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
