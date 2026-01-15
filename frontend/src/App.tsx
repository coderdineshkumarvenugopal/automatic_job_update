import JobFeed from './components/JobFeed';
import { Layout, Typography } from 'antd';
import { Briefcase } from 'lucide-react';

const { Header, Content, Footer } = Layout;
const { Title } = Typography;

function App() {
  return (
    <Layout className="min-h-screen">
      <Header className="flex items-center bg-white shadow-sm z-10 px-6">
        <Briefcase className="w-8 h-8 text-blue-600 mr-3" />
        <Title level={3} className="!mb-0 text-blue-600">Instant Job Updates</Title>
      </Header>
      <Content className="p-6 md:p-12 bg-gray-50">
        <div className="max-w-5xl mx-auto">
          <JobFeed />
        </div>
      </Content>
      <Footer className="text-center bg-gray-50 text-gray-500">
        Job Update App ©{new Date().getFullYear()} - Live Data
      </Footer>
    </Layout>
  );
}

export default App;
